package com.lldprep.systems.splitwise.policy;

import com.lldprep.systems.splitwise.exception.SplitValidationException;
import com.lldprep.systems.splitwise.model.Split;
import com.lldprep.systems.splitwise.model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PercentageSplitStrategy implements SplitStrategy {
    private static final int MONEY_SCALE = 2;
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    @Override
    public List<Split> calculate(BigDecimal amount, List<User> participants, Map<String, BigDecimal> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            throw new SplitValidationException("Percentage split needs percentage per participant");
        }

        BigDecimal normalizedAmount = normalize(amount);
        BigDecimal totalPercentage = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
        for (User participant : participants) {
            BigDecimal percentage = metadata.get(participant.getId());
            if (percentage == null) {
                throw new SplitValidationException("Missing percentage for " + participant.getName());
            }
            percentage = normalize(percentage);
            if (percentage.compareTo(BigDecimal.ZERO) < 0) {
                throw new SplitValidationException("Percentage cannot be negative for " + participant.getName());
            }
            totalPercentage = totalPercentage.add(percentage);
        }

        if (totalPercentage.compareTo(HUNDRED) != 0) {
            throw new SplitValidationException("Percentages sum to " + totalPercentage + ", must be 100.00");
        }

        BigDecimal allocated = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
        List<Split> splits = new ArrayList<>();
        for (int i = 0; i < participants.size(); i++) {
            User participant = participants.get(i);
            BigDecimal share = i == participants.size() - 1
                ? normalizedAmount.subtract(allocated)
                : normalizedAmount.multiply(normalize(metadata.get(participant.getId())))
                    .divide(HUNDRED, MONEY_SCALE, RoundingMode.DOWN);
            share = normalize(share);
            allocated = allocated.add(share);
            splits.add(new Split(participant, share));
        }
        return splits;
    }

    private BigDecimal normalize(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
