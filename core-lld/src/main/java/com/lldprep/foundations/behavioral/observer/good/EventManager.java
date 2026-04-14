package com.lldprep.foundations.behavioral.observer.good;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic event manager — a reusable Observable implementation.
 *
 * Subjects (WeatherStation, BookingService, etc.) can hold this as a field
 * and delegate subscribe/notify calls to it. This avoids code duplication
 * across every class that needs to notify observers.
 */
public class EventManager<T> implements Observable<T> {

    private final Map<String, List<Observer<T>>> listeners = new HashMap<>();

    @Override
    public void subscribe(String eventType, Observer<T> observer) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(observer);
    }

    @Override
    public void unsubscribe(String eventType, Observer<T> observer) {
        List<Observer<T>> obs = listeners.get(eventType);
        if (obs != null) obs.remove(observer);
    }

    @Override
    public void notifyObservers(String eventType, T data) {
        List<Observer<T>> obs = listeners.getOrDefault(eventType, List.of());
        for (Observer<T> observer : new ArrayList<>(obs)) { // snapshot to avoid ConcurrentModification
            observer.update(eventType, data);
        }
    }
}
