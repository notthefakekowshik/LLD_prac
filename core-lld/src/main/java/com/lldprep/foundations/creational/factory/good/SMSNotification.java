package com.lldprep.foundations.creational.factory.good;

/**
 * Concrete Product - SMS notification implementation.
 */
public class SMSNotification implements Notification {
    private final String provider;
    private final String apiKey;
    
    public SMSNotification(String provider, String apiKey) {
        this.provider = provider;
        this.apiKey = apiKey;
    }
    
    @Override
    public void send(String message) {
        System.out.println("[SMS via " + provider + "] " + message);
    }
    
    @Override
    public String getChannel() {
        return "SMS";
    }
}
