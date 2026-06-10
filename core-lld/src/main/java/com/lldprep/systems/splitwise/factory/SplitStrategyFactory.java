package com.lldprep.systems.splitwise.factory;

import com.lldprep.systems.splitwise.model.enums.SplitType;
import com.lldprep.systems.splitwise.policy.EqualSplitStrategy;
import com.lldprep.systems.splitwise.policy.ExactSplitStrategy;
import com.lldprep.systems.splitwise.policy.PercentageSplitStrategy;
import com.lldprep.systems.splitwise.policy.SplitStrategy;

import java.util.EnumMap;
import java.util.Map;

public class SplitStrategyFactory {
    private final Map<SplitType, SplitStrategy> strategies;

    public SplitStrategyFactory() {
        this.strategies = new EnumMap<>(SplitType.class);
        strategies.put(SplitType.EQUAL, new EqualSplitStrategy());
        strategies.put(SplitType.EXACT, new ExactSplitStrategy());
        strategies.put(SplitType.PERCENTAGE, new PercentageSplitStrategy());
    }

    public SplitStrategy get(SplitType splitType) {
        // Why: Factory centralizes strategy selection so expense orchestration depends on SplitStrategy, not concrete classes.
        SplitStrategy strategy = strategies.get(splitType);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported split type: " + splitType);
        }
        return strategy;
    }
}
