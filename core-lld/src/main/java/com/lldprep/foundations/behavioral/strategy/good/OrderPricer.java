package com.lldprep.foundations.behavioral.strategy.good;

/** Context for the discount strategy — mirrors how OrderService would use it. */
public class OrderPricer {

    private DiscountStrategy discountStrategy;

    public OrderPricer(DiscountStrategy discountStrategy) {
        this.discountStrategy = discountStrategy;
    }

    public void setDiscountStrategy(DiscountStrategy strategy) {
        this.discountStrategy = strategy;
    }

    public double calculateFinalPrice(double basePrice) {
        double finalPrice = discountStrategy.apply(basePrice);
        System.out.printf("  Base: %.2f | Strategy: %-20s | Final: %.2f%n",
                basePrice, discountStrategy.description(), finalPrice);
        return finalPrice;
    }
}
