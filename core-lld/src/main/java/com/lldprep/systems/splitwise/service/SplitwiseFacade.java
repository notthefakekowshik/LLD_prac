package com.lldprep.systems.splitwise.service;

import com.lldprep.systems.splitwise.exception.DuplicateEmailException;
import com.lldprep.systems.splitwise.exception.ExpenseValidationException;
import com.lldprep.systems.splitwise.exception.GroupNotFoundException;
import com.lldprep.systems.splitwise.exception.InvalidSettlementException;
import com.lldprep.systems.splitwise.exception.UserNotFoundException;
import com.lldprep.systems.splitwise.factory.SplitStrategyFactory;
import com.lldprep.systems.splitwise.model.Expense;
import com.lldprep.systems.splitwise.model.Group;
import com.lldprep.systems.splitwise.model.Settlement;
import com.lldprep.systems.splitwise.model.User;
import com.lldprep.systems.splitwise.model.enums.SplitType;
import com.lldprep.systems.splitwise.repository.ExpenseRepository;
import com.lldprep.systems.splitwise.repository.GroupRepository;
import com.lldprep.systems.splitwise.repository.SettlementRepository;
import com.lldprep.systems.splitwise.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class SplitwiseFacade {
    private static final int MONEY_SCALE = 2;

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;
    private final BalanceService balanceService;
    private final ExpenseService expenseService;
    private final List<SplitwiseEventListener> listeners;

    public SplitwiseFacade() {
        this.userRepository = new UserRepository();
        this.groupRepository = new GroupRepository();
        this.expenseRepository = new ExpenseRepository();
        this.settlementRepository = new SettlementRepository();
        this.balanceService = new BalanceService();
        SplitStrategyFactory splitStrategyFactory = new SplitStrategyFactory();
        this.expenseService = new ExpenseService(this.userRepository, this.groupRepository,
            this.expenseRepository, this.balanceService, splitStrategyFactory);
        this.listeners = new CopyOnWriteArrayList<>();
    }

    // Why: DIP — callers inject all dependencies; no concrete types created internally.
    public SplitwiseFacade(UserRepository userRepository, GroupRepository groupRepository,
                           ExpenseRepository expenseRepository, SettlementRepository settlementRepository,
                           BalanceService balanceService, ExpenseService expenseService) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.expenseRepository = expenseRepository;
        this.settlementRepository = settlementRepository;
        this.balanceService = balanceService;
        this.expenseService = expenseService;
        this.listeners = new CopyOnWriteArrayList<>();
    }

    public void addListener(SplitwiseEventListener listener) {
        listeners.add(listener);
    }

    public User registerUser(String name, String email) {
        validateUserInput(name, email);
        User user = new User(name, email);
        if (!userRepository.saveIfEmailAbsent(user)) {
            throw new DuplicateEmailException(email);
        }
        return user;
    }

    public Group createGroup(String name, List<String> memberIds) {
        if (name == null || name.isBlank()) {
            throw new ExpenseValidationException("Group name cannot be empty");
        }
        List<User> members = resolveUsers(memberIds);
        Group group = new Group(name, members);
        groupRepository.save(group);
        return group;
    }

    public void addMember(String groupId, String userId) {
        Group group = getGroupOrThrow(groupId);
        group.addMember(getUserOrThrow(userId));
    }

    public Expense addExpense(String description, BigDecimal amount, String paidByUserId, List<String> participantIds,
                              SplitType splitType, Map<String, BigDecimal> metadata, String groupId) {
        Expense expense = expenseService.addExpense(description, amount, paidByUserId, participantIds, splitType, metadata, groupId);
        fireExpenseAdded(expense);
        return expense;
    }

    public Settlement settle(String payerId, String payeeId, BigDecimal amount) {
        if (payerId.equals(payeeId)) {
            throw new InvalidSettlementException("Payer and payee cannot be same user");
        }
        User payer = getUserOrThrow(payerId);
        User payee = getUserOrThrow(payeeId);
        BigDecimal normalizedAmount = normalize(amount);
        if (normalizedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidSettlementException("Settlement amount must be positive");
        }

        Settlement settlement = new Settlement(payer, payee, normalizedAmount);
        balanceService.updateOnSettlement(settlement);
        settlementRepository.save(settlement);
        fireSettlementRecorded(settlement);
        return settlement;
    }

    public BigDecimal getBalance(String userA, String userB) {
        getUserOrThrow(userA);
        getUserOrThrow(userB);
        return balanceService.getBalance(userA, userB);
    }

    public Map<String, BigDecimal> getBalanceSummary(String userId) {
        getUserOrThrow(userId);
        return balanceService.getBalanceSummary(userId);
    }

    public List<Expense> getExpensesForUser(String userId) {
        return expenseService.getExpensesForUser(userId);
    }

    public List<Expense> getExpensesForGroup(String groupId) {
        return expenseService.getExpensesForGroup(groupId);
    }

    public List<Settlement> simplifyDebts(String groupId) {
        Group group = getGroupOrThrow(groupId);
        return balanceService.simplifyDebts(group.getMembers());
    }

    public int totalUsers() {
        return userRepository.count();
    }

    public int totalGroups() {
        return groupRepository.count();
    }

    public int totalExpenses() {
        return expenseRepository.count();
    }

    public int totalSettlements() {
        return settlementRepository.count();
    }

    public User getUser(String userId) {
        return getUserOrThrow(userId);
    }

    private List<User> resolveUsers(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new ExpenseValidationException("At least one group member is required");
        }
        List<User> users = new ArrayList<>();
        for (String userId : userIds) {
            users.add(getUserOrThrow(userId));
        }
        return users;
    }

    private void validateUserInput(String name, String email) {
        if (name == null || name.isBlank()) {
            throw new ExpenseValidationException("User name cannot be empty");
        }
        if (email == null || !email.contains("@")) {
            throw new ExpenseValidationException("Invalid email: " + email);
        }
    }

    private User getUserOrThrow(String userId) {
        User user = userRepository.getById(userId);
        if (user == null) {
            throw new UserNotFoundException(userId);
        }
        return user;
    }

    private Group getGroupOrThrow(String groupId) {
        Group group = groupRepository.getById(groupId);
        if (group == null) {
            throw new GroupNotFoundException(groupId);
        }
        return group;
    }

    private void fireExpenseAdded(Expense expense) {
        for (SplitwiseEventListener listener : listeners) {
            listener.onExpenseAdded(expense);
        }
    }

    private void fireSettlementRecorded(Settlement settlement) {
        for (SplitwiseEventListener listener : listeners) {
            listener.onSettlementRecorded(settlement);
        }
    }

    private BigDecimal normalize(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
