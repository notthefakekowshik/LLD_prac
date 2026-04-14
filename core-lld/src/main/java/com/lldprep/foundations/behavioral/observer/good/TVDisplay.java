package com.lldprep.foundations.behavioral.observer.good;

public class TVDisplay implements Observer<Double> {

    @Override
    public void update(String eventType, Double data) {
        System.out.printf("  [TVDisplay] Broadcasting %s: %.1f°%n", eventType, data);
    }
}
