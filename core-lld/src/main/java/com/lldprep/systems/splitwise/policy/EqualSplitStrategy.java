package com.lldprep.systems.splitwise.policy;

import com.lldprep.systems.splitwise.exception.SplitValidationException;
import com.lldprep.systems.splitwise.model.Split;
import com.lldprep.systems.splitwise.model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EqualSplitStrategy implements SplitStrategy {
    private static final int MONEY_SCALE = 2;

    @Override
    public List<Split> calculate(BigDecimal amount, List<User> participants, Map<String, BigDecimal> metadata) {
        if (participants == null || participants.isEmpty()) {
            throw new SplitValidationException("Equal split needs at least one participant");
        }

        BigDecimal normalizedAmount = normalize(amount);
        BigDecimal baseShare = normalizedAmount.divide(
            BigDecimal.valueOf(participants.size()), MONEY_SCALE, RoundingMode.DOWN);
        BigDecimal allocated = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);

        List<Split> splits = new ArrayList<>();
        for (int i = 0; i < participants.size(); i++) {
            BigDecimal share = i == participants.size() - 1
                ? normalizedAmount.subtract(allocated)
                : baseShare;
            share = normalize(share);
            allocated = allocated.add(share);
            splits.add(new Split(participants.get(i), share));
        }
        return splits;
    }

    private BigDecimal normalize(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
