package com.lldprep.foundations.creational.factory.good;

/**
 * Product Interface - common contract for all notifications.
 */
public interface Notification {
    void send(String message);
    String getChannel();
}
