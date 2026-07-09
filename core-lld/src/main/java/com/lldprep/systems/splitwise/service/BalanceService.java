package com.lldprep.systems.splitwise.service;

import com.lldprep.systems.splitwise.exception.InvalidSettlementException;
import com.lldprep.systems.splitwise.model.Expense;
import com.lldprep.systems.splitwise.model.Settlement;
import com.lldprep.systems.splitwise.model.Split;
import com.lldprep.systems.splitwise.model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BalanceService {
    private static final int MONEY_SCALE = 2;

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, BigDecimal>> balances = new ConcurrentHashMap<>();
    // Trade-off: locks are retained per user pair for simple, stable monitor identity in this in-memory LLD.
    private final ConcurrentHashMap<String, Object> pairLocks = new ConcurrentHashMap<>();

    public void updateOnExpense(Expense expense) {
        User paidBy = expense.getPaidBy();
        for (Split split : expense.getSplits()) {
            if (!split.getUser().equals(paidBy) && split.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                addDebt(split.getUser().getId(), paidBy.getId(), split.getAmount());
            }
        }
    }

    public void updateOnSettlement(Settlement settlement) {
        String payerId = settlement.getSender().getId();
        String payeeId = settlement.getReceiver().getId();
        BigDecimal amount = normalize(settlement.getAmount());

        synchronized (getLock(payerId, payeeId)) {
            BigDecimal current = getBalanceUnsafe(payerId, payeeId);
            if (current.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidSettlementException(settlement.getSender().getName()
                    + " does not owe " + settlement.getReceiver().getName());
            }
            if (amount.compareTo(current) > 0) {
                throw new InvalidSettlementException("Settlement amount " + amount
                    + " exceeds outstanding balance " + current);
            }
            setBalanceUnsafe(payerId, payeeId, current.subtract(amount));
        }
    }

    public BigDecimal getBalance(String userA, String userB) {
        synchronized (getLock(userA, userB)) {
            return getBalanceUnsafe(userA, userB);
        }
    }

    public Map<String, BigDecimal> getBalanceSummary(String userId) {
        Map<String, BigDecimal> summary = new HashMap<>();
        Map<String, Map<String, BigDecimal>> snapshot = snapshotBalances();

        for (Map.Entry<String, Map<String, BigDecimal>> debtorEntry : snapshot.entrySet()) {
            String debtorId = debtorEntry.getKey();
            for (Map.Entry<String, BigDecimal> creditorEntry : debtorEntry.getValue().entrySet()) {
                String creditorId = creditorEntry.getKey();
                BigDecimal amount = creditorEntry.getValue();
                if (debtorId.equals(userId)) {
                    summary.merge(creditorId, amount.negate(), BigDecimal::add);
                } else if (creditorId.equals(userId)) {
                    summary.merge(debtorId, amount, BigDecimal::add);
                }
            }
        }

        return summary.entrySet().stream()
            .filter(entry -> entry.getValue().compareTo(BigDecimal.ZERO) != 0)
            .sorted(Map.Entry.comparingByKey())
            .collect(LinkedHashMap::new,
                (map, entry) -> map.put(entry.getKey(), normalize(entry.getValue())),
                LinkedHashMap::putAll);
    }

    public List<Settlement> simplifyDebts(Collection<User> users) {
        Map<String, User> userById = new HashMap<>();
        for (User user : users) {
            userById.put(user.getId(), user);
        }

        Map<String, BigDecimal> net = new HashMap<>();
        for (User user : users) {
            net.put(user.getId(), BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY));
        }

        Set<String> includedUserIds = userById.keySet();
        Map<String, Map<String, BigDecimal>> snapshot = snapshotBalances();
        for (Map.Entry<String, Map<String, BigDecimal>> debtorEntry : snapshot.entrySet()) {
            String debtorId = debtorEntry.getKey();
            for (Map.Entry<String, BigDecimal> creditorEntry : debtorEntry.getValue().entrySet()) {
                String creditorId = creditorEntry.getKey();
                BigDecimal amount = creditorEntry.getValue();
                if (includedUserIds.contains(debtorId) && includedUserIds.contains(creditorId)) {
                    net.merge(debtorId, amount.negate(), BigDecimal::add);
                    net.merge(creditorId, amount, BigDecimal::add);
                }
            }
        }

        PriorityQueue<NetAmount> creditors = new PriorityQueue<>(
            Comparator.comparing(NetAmount::amount).reversed());
        PriorityQueue<NetAmount> debtors = new PriorityQueue<>(
            Comparator.comparing(NetAmount::amount));

        for (Map.Entry<String, BigDecimal> entry : net.entrySet()) {
            BigDecimal amount = normalize(entry.getValue());
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(new NetAmount(entry.getKey(), amount));
            } else if (amount.compareTo(BigDecimal.ZERO) < 0) {
                debtors.add(new NetAmount(entry.getKey(), amount));
            }
        }

        List<Settlement> suggestions = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            NetAmount creditor = creditors.poll();
            NetAmount debtor = debtors.poll();
            BigDecimal settlementAmount = creditor.amount().min(debtor.amount().abs());

            suggestions.add(new Settlement(
                userById.get(debtor.userId()),
                userById.get(creditor.userId()),
                normalize(settlementAmount)));

            BigDecimal remainingCredit = creditor.amount().subtract(settlementAmount);
            BigDecimal remainingDebt = debtor.amount().add(settlementAmount);

            if (remainingCredit.compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(new NetAmount(creditor.userId(), normalize(remainingCredit)));
            }
            if (remainingDebt.compareTo(BigDecimal.ZERO) < 0) {
                debtors.add(new NetAmount(debtor.userId(), normalize(remainingDebt)));
            }
        }
        return suggestions;
    }

    private void addDebt(String debtorId, String creditorId, BigDecimal amount) {
        BigDecimal normalizedAmount = normalize(amount);
        synchronized (getLock(debtorId, creditorId)) {
            BigDecimal current = getBalanceUnsafe(debtorId, creditorId);
            setBalanceUnsafe(debtorId, creditorId, current.add(normalizedAmount));
        }
    }

    private BigDecimal getBalanceUnsafe(String userA, String userB) {
        BigDecimal forward = balances.getOrDefault(userA, new ConcurrentHashMap<>()).get(userB);
        if (forward != null) {
            return normalize(forward);
        }
        BigDecimal reverse = balances.getOrDefault(userB, new ConcurrentHashMap<>()).get(userA);
        return reverse == null ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY) : normalize(reverse.negate());
    }

    private void setBalanceUnsafe(String debtorId, String creditorId, BigDecimal signedAmount) {
        removePairUnsafe(debtorId, creditorId);
        BigDecimal normalizedAmount = normalize(signedAmount);
        if (normalizedAmount.compareTo(BigDecimal.ZERO) > 0) {
            putPositiveBalance(debtorId, creditorId, normalizedAmount);
        } else if (normalizedAmount.compareTo(BigDecimal.ZERO) < 0) {
            putPositiveBalance(creditorId, debtorId, normalizedAmount.abs());
        }
    }

    private void putPositiveBalance(String debtorId, String creditorId, BigDecimal amount) {
        balances.computeIfAbsent(debtorId, key -> new ConcurrentHashMap<>()).put(creditorId, normalize(amount));
    }

    private void removePairUnsafe(String userA, String userB) {
        removeBalance(userA, userB);
        removeBalance(userB, userA);
    }

    private void removeBalance(String debtorId, String creditorId) {
        ConcurrentHashMap<String, BigDecimal> inner = balances.get(debtorId);
        if (inner != null) {
            inner.remove(creditorId);
            if (inner.isEmpty()) {
                balances.remove(debtorId, inner);
            }
        }
    }

    private Map<String, Map<String, BigDecimal>> snapshotBalances() {
        // Weakly consistent snapshot: fine for display/read-only simplification, not a transaction boundary.
        Map<String, Map<String, BigDecimal>> snapshot = new HashMap<>();
        for (Map.Entry<String, ConcurrentHashMap<String, BigDecimal>> outer : balances.entrySet()) {
            snapshot.put(outer.getKey(), new HashMap<>(outer.getValue()));
        }
        return snapshot;
    }

    private Object getLock(String userA, String userB) {
        return pairLocks.computeIfAbsent(lockKey(userA, userB), key -> new Object());
    }

    private String lockKey(String userA, String userB) {
        return userA.compareTo(userB) <= 0 ? userA + ":" + userB : userB + ":" + userA;
    }

    private BigDecimal normalize(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private record NetAmount(String userId, BigDecimal amount) {
    }
}
