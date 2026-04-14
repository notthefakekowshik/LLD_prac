package com.lldprep.foundations.behavioral.observer.good;

/** Observer that fires an alert when temperature crosses a threshold. */
public class AlertSystem implements Observer<Double> {

    private final double threshold;

    public AlertSystem(double threshold) { this.threshold = threshold; }

    @Override
    public void update(String eventType, Double data) {
        if (WeatherStation.TEMPERATURE_CHANGED.equals(eventType) && data > threshold) {
            System.out.printf("  [ALERT] Temperature %.1f exceeds threshold %.1f! %n", data, threshold);
        }
    }
}
