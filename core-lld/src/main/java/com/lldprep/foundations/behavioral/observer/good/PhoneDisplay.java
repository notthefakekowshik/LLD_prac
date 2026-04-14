package com.lldprep.foundations.behavioral.observer.good;

public class PhoneDisplay implements Observer<Double> {
    private final String owner;

    public PhoneDisplay(String owner) { this.owner = owner; }

    @Override
    public void update(String eventType, Double data) {
        System.out.printf("  [PhoneDisplay - %s] %s → %.1f%n", owner, eventType, data);
    }
}
