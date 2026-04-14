package com.lldprep.foundations.behavioral.observer.good;

/**
 * Subject — publishes weather events. Knows nothing about who is listening.
 * All notification is delegated to EventManager.
 */
public class WeatherStation {

    public static final String TEMPERATURE_CHANGED = "TEMPERATURE_CHANGED";
    public static final String HUMIDITY_CHANGED    = "HUMIDITY_CHANGED";

    private final EventManager<Double> events = new EventManager<>();
    private double temperature;
    private double humidity;

    public void subscribe(String eventType, Observer<Double> observer) {
        events.subscribe(eventType, observer);
    }

    public void unsubscribe(String eventType, Observer<Double> observer) {
        events.unsubscribe(eventType, observer);
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
        events.notifyObservers(TEMPERATURE_CHANGED, temperature);
    }

    public void setHumidity(double humidity) {
        this.humidity = humidity;
        events.notifyObservers(HUMIDITY_CHANGED, humidity);
    }
}
