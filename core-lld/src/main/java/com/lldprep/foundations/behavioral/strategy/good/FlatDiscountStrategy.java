package com.lldprep.foundations.behavioral.strategy.good;

public class FlatDiscountStrategy implements DiscountStrategy {

    private final double flatAmount;

    public FlatDiscountStrategy(double flatAmount) {
        this.flatAmount = flatAmount;
    }

    @Override
    public double apply(double originalPrice) {
        return Math.max(0, originalPrice - flatAmount);
    }

    @Override
    public String description() { return "Flat ₹" + flatAmount + " off"; }
}
