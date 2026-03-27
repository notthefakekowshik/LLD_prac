package com.lldprep.foundations.oop.interfaces.bad;

/**
 * BAD CODE: Another concrete class with completely different method signature.
 * 
 * Notice the problem:
 * - EmailNotifier has sendEmail(recipient, subject, body)
 * - SMSNotifier has sendSMS(phoneNumber, message)
 * 
 * There's NO common contract, so you cannot write polymorphic code.
 */
public class SMSNotifier {
    
    private String apiKey;
    private String provider;
    
    public SMSNotifier(String apiKey, String provider) {
        this.apiKey = apiKey;
        this.provider = provider;
    }
    
    public void sendSMS(String phoneNumber, String message) {
        System.out.println("[SMS] Using provider: " + provider);
        System.out.println("[SMS] API Key: " + apiKey.substring(0, 4) + "****");
        System.out.println("[SMS] To: " + phoneNumber);
        System.out.println("[SMS] Message: " + message);
        System.out.println("[SMS] SMS sent successfully!");
    }
    
    public String getProvider() {
        return provider;
    }
}
