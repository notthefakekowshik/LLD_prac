package com.lldprep.foundations.oop.interfaces.good;

/**
 * GOOD CODE: SMS implementation of Notifier interface.
 * 
 * Notice: The method signature is now CONSISTENT with EmailNotifier.
 * Both implement send(recipient, subject, message).
 * 
 * This enables polymorphism - we can treat all notifiers uniformly.
 */
public class SMSNotifier implements Notifier {
    
    private String apiKey;
    private String provider;
    
    public SMSNotifier(String apiKey, String provider) {
        this.apiKey = apiKey;
        this.provider = provider;
    }
    
    @Override
    public void send(String recipient, String subject, String message) {
        System.out.println("[SMS] Using provider: " + provider);
        System.out.println("[SMS] API Key: " + apiKey.substring(0, 4) + "****");
        System.out.println("[SMS] To: " + recipient);
        System.out.println("[SMS] Message: " + subject + " - " + message);
        System.out.println("[SMS] SMS sent successfully!");
    }
    
    @Override
    public String getChannelType() {
        return "SMS";
    }
    
    public String getProvider() {
        return provider;
    }
}
