package com.lldprep.systems.splitwise.policy;

import com.lldprep.systems.splitwise.exception.SplitValidationException;
import com.lldprep.systems.splitwise.model.Split;
import com.lldprep.systems.splitwise.model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExactSplitStrategy implements SplitStrategy {
    private static final int MONEY_SCALE = 2;

    @Override
    public List<Split> calculate(BigDecimal amount, List<User> participants, Map<String, BigDecimal> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            throw new SplitValidationException("Exact split needs amount per participant");
        }

        BigDecimal expectedTotal = normalize(amount);
        BigDecimal actualTotal = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
        List<Split> splits = new ArrayList<>();

        for (User participant : participants) {
            BigDecimal participantAmount = metadata.get(participant.getId());
            if (participantAmount == null) {
                throw new SplitValidationException("Missing exact amount for " + participant.getName());
            }
            participantAmount = normalize(participantAmount);
            if (participantAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new SplitValidationException("Split amount cannot be negative for " + participant.getName());
            }
            actualTotal = actualTotal.add(participantAmount);
            splits.add(new Split(participant, participantAmount));
        }

        if (actualTotal.compareTo(expectedTotal) != 0) {
            throw new SplitValidationException("Exact amounts sum to " + actualTotal + " but expense is " + expectedTotal);
        }
        return splits;
    }

    private BigDecimal normalize(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
