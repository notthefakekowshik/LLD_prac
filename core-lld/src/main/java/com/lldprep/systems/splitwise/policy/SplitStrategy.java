package com.lldprep.systems.splitwise.policy;

import com.lldprep.systems.splitwise.model.Split;
import com.lldprep.systems.splitwise.model.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface SplitStrategy {
    List<Split> calculate(BigDecimal amount, List<User> participants, Map<String, BigDecimal> metadata);
}
