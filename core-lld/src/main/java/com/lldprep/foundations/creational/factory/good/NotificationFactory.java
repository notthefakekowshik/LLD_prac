package com.lldprep.foundations.creational.factory.good;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Simple Factory - Centralized object creation.
 * 
 * BENEFITS:
 * 1. Single place to modify when adding new types
 * 2. Client decoupled from concrete classes
 * 3. Easy to add validation/logging during creation
 * 4. Can use registry pattern for dynamic registration
 */
public class NotificationFactory {
    
    // Static factory method - simplest approach
    public static Notification createNotification(String type) {
        return switch (type.toUpperCase()) {
            case "EMAIL" -> new EmailNotification("smtp.gmail.com", 587);
            case "SMS" -> new SMSNotification("Twilio", "api-key-123");
            case "PUSH" -> new PushNotification("fcm-token-abc");
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }
    
    // Factory with configuration - more flexible
    public static Notification createNotification(String type, Map<String, String> config) {
        return switch (type.toUpperCase()) {
            case "EMAIL" -> new EmailNotification(
                config.getOrDefault("smtp", "default.smtp.com"),
                Integer.parseInt(config.getOrDefault("port", "587"))
            );
            case "SMS" -> new SMSNotification(
                config.getOrDefault("provider", "DefaultSMS"),
                config.getOrDefault("apiKey", "default-key")
            );
            case "PUSH" -> new PushNotification(
                config.getOrDefault("fcmToken", "default-token")
            );
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }
}
