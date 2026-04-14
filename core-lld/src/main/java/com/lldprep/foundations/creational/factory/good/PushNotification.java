package com.lldprep.foundations.creational.factory.good;

/**
 * Concrete Product - Push notification implementation.
 */
public class PushNotification implements Notification {
    private final String fcmToken;
    
    public PushNotification(String fcmToken) {
        this.fcmToken = fcmToken;
    }
    
    @Override
    public void send(String message) {
        System.out.println("[PUSH to " + fcmToken.substring(0, 10) + "...] " + message);
    }
    
    @Override
    public String getChannel() {
        return "PUSH";
    }
}
