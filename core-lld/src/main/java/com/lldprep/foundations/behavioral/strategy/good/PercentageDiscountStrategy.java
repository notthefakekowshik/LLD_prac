package com.lldprep.foundations.behavioral.strategy.good;

public class PercentageDiscountStrategy implements DiscountStrategy {

    private final double percent; // e.g., 20.0 means 20% off

    public PercentageDiscountStrategy(double percent) {
        this.percent = percent;
    }

    @Override
    public double apply(double originalPrice) {
        return originalPrice * (1 - percent / 100);
    }

    @Override
    public String description() { return percent + "% off"; }
}
