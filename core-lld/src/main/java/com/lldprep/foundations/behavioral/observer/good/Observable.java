package com.lldprep.foundations.behavioral.observer.good;

/**
 * Subject/Observable interface — anything that can be observed.
 * Separating this into an interface allows multiple subjects to be observable.
 */
public interface Observable<T> {
    void subscribe(String eventType, Observer<T> observer);
    void unsubscribe(String eventType, Observer<T> observer);
    void notifyObservers(String eventType, T data);
}
