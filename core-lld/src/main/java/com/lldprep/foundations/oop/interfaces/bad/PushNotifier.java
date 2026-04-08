package com.lldprep.foundations.oop.interfaces.bad;

/**
 * BAD CODE: Yet another concrete class with its own unique method signature.
 * 
 * Now we have THREE different methods:
 * - EmailNotifier.sendEmail(recipient, subject, body)
 * - SMSNotifier.sendSMS(phoneNumber, message)
 * - PushNotifier.sendPushNotification(deviceToken, title, body, data)
 * 
 * This makes it IMPOSSIBLE to write generic notification code.
 */
public class PushNotifier {
    
    private String fcmServerKey;
    
    public PushNotifier(String fcmServerKey) {
        this.fcmServerKey = fcmServerKey;
    }
    
    public void sendPushNotification(String deviceToken, String title, String body, String data) {
        System.out.println("[PUSH] Using FCM Server Key: " + fcmServerKey.substring(0, 4) + "****");
        System.out.println("[PUSH] Device Token: " + deviceToken);
        System.out.println("[PUSH] Title: " + title);
        System.out.println("[PUSH] Body: " + body);
        System.out.println("[PUSH] Data: " + data);
        System.out.println("[PUSH] Push notification sent successfully!");
    }
    
    public String getFcmServerKey() {
        return fcmServerKey;
    }
}
