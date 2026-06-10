package com.lldprep.systems.splitwise.repository;

import com.lldprep.systems.splitwise.model.Settlement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SettlementRepository {
    private final List<Settlement> settlements = new CopyOnWriteArrayList<>();

    public void save(Settlement settlement) {
        settlements.add(settlement);
    }

    public List<Settlement> findByUserPair(String userA, String userB) {
        return settlements.stream()
            .filter(settlement -> isPair(settlement, userA, userB))
            .sorted(Comparator.comparing(Settlement::getSettledAt))
            .toList();
    }

    public List<Settlement> getAll() {
        return new ArrayList<>(settlements);
    }

    public int count() {
        return settlements.size();
    }

    private boolean isPair(Settlement settlement, String userA, String userB) {
        String payerId = settlement.getPayer().getId();
        String payeeId = settlement.getPayee().getId();
        return (payerId.equals(userA) && payeeId.equals(userB))
            || (payerId.equals(userB) && payeeId.equals(userA));
    }
}
