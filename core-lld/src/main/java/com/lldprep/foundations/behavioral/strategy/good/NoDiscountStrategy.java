package com.lldprep.foundations.behavioral.strategy.good;

public class NoDiscountStrategy implements DiscountStrategy {
    @Override
    public double apply(double originalPrice) { return originalPrice; }

    @Override
    public String description() { return "No discount"; }
}
