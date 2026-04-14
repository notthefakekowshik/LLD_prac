package com.lldprep.foundations.behavioral.observer.good;

/**
 * Observer interface — implemented by anything that wants to react to events.
 *
 * @param <T> the type of data the event carries
 */
public interface Observer<T> {
    void update(String eventType, T data);
}
