package com.lldprep.foundations.behavioral.strategy.good;

/**
 * Second real-world example of Strategy: pricing/discount logic.
 *
 * In e-commerce, discount rules change constantly (seasonal sales, loyalty tiers, coupons).
 * Without Strategy, every new rule requires editing a giant if-else block in OrderService.
 * With Strategy, each rule is a separate class — add new rules without touching existing ones.
 */
public interface DiscountStrategy {
    double apply(double originalPrice);
    String description();
}
