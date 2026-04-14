package com.lldprep.foundations.creational.factory.good;

import java.util.Map;

/**
 * FACTORY PATTERN
 * ===============
 *
 * WHY IT EXISTS:
 * Centralizes object creation logic to decouple client code from concrete classes.
 * Instead of using "new" directly, clients ask the factory to create objects.
 *
 * PROBLEMS IT SOLVES:
 * - Client code tightly coupled to specific implementation classes
 * - Violation of Open/Closed Principle (modifying client to add new types)
 * - Duplicated instantiation logic scattered across codebase
 * - Hard to test (cannot easily mock concrete implementations)
 * - Complex object creation logic mixed with business logic
 *
 * WHEN TO USE:
 * - When you don't know beforehand what exact types of objects your code will work with
 * - When you want to provide users with extension points to add new types
 * - When you want to reuse existing objects instead of rebuilding them
 * - When creating objects is complex (requires configuration, validation)
 * - Frameworks and libraries (Spring, JDBC drivers, UI toolkits)
 *
 * VARIATIONS SHOWN:
 * - Simple Static Factory: Centralized creation with if-else/switch
 * - Factory Method: Subclasses decide which object to instantiate
 * - Dynamic Registry: Runtime registration of new types
 *
 * REAL-WORLD EXAMPLES:
 * - JDBC DriverManager.getConnection()
 * - Spring BeanFactory
 * - Java Calendar.getInstance()
 * - Slf4j LoggerFactory
 *
 * @see <a href="https://en.wikipedia.org/wiki/Factory_method_pattern">Factory Pattern</a>
 */
public class FactoryDemo {
    
    public static void main(String[] args) {
        System.out.println("=== FACTORY PATTERN DEMONSTRATIONS ===\n");
        
        // 1. Simple Static Factory
        System.out.println("1. SIMPLE STATIC FACTORY:");
        Notification email = NotificationFactory.createNotification("EMAIL");
        email.send("Hello via Factory!");
        
        Notification sms = NotificationFactory.createNotification("SMS");
        sms.send("SMS via Factory!");
        System.out.println();
        
        // 2. Factory with Configuration
        System.out.println("2. FACTORY WITH CONFIGURATION:");
        Map<String, String> emailConfig = Map.of(
            "smtp", "smtp.outlook.com",
            "port", "465"
        );
        Notification customEmail = NotificationFactory.createNotification("EMAIL", emailConfig);
        customEmail.send("Custom config email");
        System.out.println();
        
        // 3. Factory Method Pattern
        System.out.println("3. FACTORY METHOD PATTERN:");
        FactoryMethodCreator emailCreator = new EmailNotificationCreator();
        emailCreator.notifyUser("Factory Method pattern!");
        
        FactoryMethodCreator smsCreator = new SMSNotificationCreator();
        smsCreator.notifyUser("SMS via Factory Method!");
        System.out.println();
        
        // 4. Dynamic Registry Factory
        System.out.println("4. DYNAMIC REGISTRY FACTORY:");
        DynamicFactoryRegistry registry = new DynamicFactoryRegistry();
        
        // Register custom notification type at runtime
        registry.register("SLACK", config -> new Notification() {
            @Override
            public void send(String message) {
                System.out.println("[SLACK to " + config.get("channel") + "] " + message);
            }
            @Override
            public String getChannel() { return "SLACK"; }
        });
        
        Notification slack = registry.create("SLACK", Map.of("channel", "#general"));
        slack.send("Hello from dynamic registry!");
        System.out.println();
        
        // 5. Dependency Injection style usage
        System.out.println("5. INJECTION STYLE (Decoupled Client):");
        NotificationService service = new NotificationService(
            NotificationFactory.createNotification("EMAIL")
        );
        service.sendAlert("System alert!");
        
        System.out.println("\n=== BENEFITS ===");
        System.out.println("- Client code doesn't know concrete classes");
        System.out.println("- Adding new type requires changes only in factory");
        System.out.println("- Easy to mock for testing");
        System.out.println("- Can add caching/logging/validation in one place");
    }
}

// Example of how factory enables dependency injection
class NotificationService {
    private final Notification notification;
    
    public NotificationService(Notification notification) {
        this.notification = notification;
    }
    
    public void sendAlert(String message) {
        notification.send("[ALERT] " + message);
    }
}
