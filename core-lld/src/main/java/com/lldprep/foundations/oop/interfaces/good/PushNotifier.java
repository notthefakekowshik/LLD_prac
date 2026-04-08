package com.lldprep.foundations.oop.interfaces.good;

/**
 * GOOD CODE: Push notification implementation of Notifier interface.
 * 
 * All three notifiers (Email, SMS, Push) now share the SAME contract.
 * This is the power of interfaces - unified behavior across different implementations.
 */
public class PushNotifier implements Notifier {
    
    private String fcmServerKey;
    
    public PushNotifier(String fcmServerKey) {
        this.fcmServerKey = fcmServerKey;
    }
    
    @Override
    public void send(String recipient, String subject, String message) {
        System.out.println("[PUSH] Using FCM Server Key: " + fcmServerKey.substring(0, 4) + "****");
        System.out.println("[PUSH] Device Token: " + recipient);
        System.out.println("[PUSH] Title: " + subject);
        System.out.println("[PUSH] Body: " + message);
        System.out.println("[PUSH] Push notification sent successfully!");
    }
    
    @Override
    public String getChannelType() {
        return "PUSH";
    }
    
    public String getFcmServerKey() {
        return fcmServerKey;
    }
}
