package com.lldprep.foundations.creational.factory.good;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Dynamic Factory using Registry Pattern.
 * 
 * ADVANCED: Register creators at runtime. Useful for:
 * - Plugin architectures
 * - Runtime configuration
 * - Dependency injection frameworks
 */
public class DynamicFactoryRegistry {
    
    private final Map<String, Function<Map<String, String>, Notification>> registry = new HashMap<>();
    
    public DynamicFactoryRegistry() {
        // Register default creators
        register("EMAIL", config -> new EmailNotification(
            config.getOrDefault("smtp", "smtp.gmail.com"),
            Integer.parseInt(config.getOrDefault("port", "587"))
        ));
        register("SMS", config -> new SMSNotification(
            config.getOrDefault("provider", "Twilio"),
            config.getOrDefault("apiKey", "default")
        ));
        register("PUSH", config -> new PushNotification(
            config.getOrDefault("fcmToken", "default")
        ));
    }
    
    // Register new type dynamically
    public void register(String type, Function<Map<String, String>, Notification> creator) {
        registry.put(type.toUpperCase(), creator);
    }
    
    public Notification create(String type, Map<String, String> config) {
        Function<Map<String, String>, Notification> creator = registry.get(type.toUpperCase());
        if (creator == null) {
            throw new IllegalArgumentException("No creator registered for: " + type);
        }
        return creator.apply(config);
    }
    
    public boolean isRegistered(String type) {
        return registry.containsKey(type.toUpperCase());
    }
}
