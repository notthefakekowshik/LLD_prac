package com.lldprep.foundations.oop.interfaces;

import com.lldprep.foundations.oop.interfaces.bad.*;
import com.lldprep.foundations.oop.interfaces.good.*;

import java.util.List;

/**
 * Demonstrates the critical importance of interfaces in OOP design.
 * 
 * This example shows:
 * - BAD: Tight coupling to concrete classes
 * - GOOD: Loose coupling via interfaces
 * 
 * Key Learning: "Program to an interface, not an implementation"
 */
public class InterfacesDemo {
    
    public static void main(String[] args) {
        System.out.println("===== INTERFACES: BAD vs GOOD =====\n");
        
        demonstrateBadApproach();
        demonstrateGoodApproach();
        demonstrateFlexibility();
        demonstrateTestability();
        demonstrateExtensibility();
        
        System.out.println("\n===== KEY TAKEAWAYS =====");
        printKeyTakeaways();
    }
    
    /**
     * BAD APPROACH: No interfaces, tight coupling to concrete classes.
     */
    private static void demonstrateBadApproach() {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║              BAD: WITHOUT INTERFACES                      ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");
        
        System.out.println("PROBLEMS:");
        System.out.println("  1. OrderService is TIGHTLY COUPLED to EmailNotifier, SMSNotifier, PushNotifier");
        System.out.println("  2. Each notifier has DIFFERENT method signatures:");
        System.out.println("     - EmailNotifier.sendEmail(recipient, subject, body)");
        System.out.println("     - SMSNotifier.sendSMS(phoneNumber, message)");
        System.out.println("     - PushNotifier.sendPushNotification(token, title, body, data)");
        System.out.println("  3. Cannot write POLYMORPHIC code (no common interface)");
        System.out.println("  4. Cannot LOOP over notifiers (each requires separate method call)");
        System.out.println("  5. Hard to TEST (cannot mock easily)");
        System.out.println("  6. Violates OPEN/CLOSED PRINCIPLE (adding Slack requires modifying OrderService)");
        System.out.println();
        
        // Must create all three concrete types
        com.lldprep.foundations.oop.interfaces.bad.EmailNotifier badEmail = 
            new com.lldprep.foundations.oop.interfaces.bad.EmailNotifier("smtp.example.com", 587);
        com.lldprep.foundations.oop.interfaces.bad.SMSNotifier badSMS = 
            new com.lldprep.foundations.oop.interfaces.bad.SMSNotifier("api-key-123", "Twilio");
        com.lldprep.foundations.oop.interfaces.bad.PushNotifier badPush = 
            new com.lldprep.foundations.oop.interfaces.bad.PushNotifier("fcm-server-key-456");
        
        // OrderService is forced to accept all three concrete types
        com.lldprep.foundations.oop.interfaces.bad.OrderService badService = 
            new com.lldprep.foundations.oop.interfaces.bad.OrderService(badEmail, badSMS, badPush);
        
        badService.placeOrder("user123", "ORD-001", 99.99);
        
        System.out.println("❌ RESULT: Rigid, tightly coupled, hard to maintain\n");
        System.out.println("=".repeat(80) + "\n");
    }
    
    /**
     * GOOD APPROACH: Using interfaces for loose coupling and polymorphism.
     */
    private static void demonstrateGoodApproach() {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║               GOOD: WITH INTERFACES                       ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");
        
        System.out.println("SOLUTIONS:");
        System.out.println("  1. Notifier INTERFACE defines a common contract");
        System.out.println("  2. All notifiers implement send(recipient, subject, message)");
        System.out.println("  3. OrderService depends on Notifier INTERFACE, not concrete classes");
        System.out.println("  4. Can LOOP over List<Notifier> - polymorphism in action");
        System.out.println("  5. Easy to TEST with MockNotifier");
        System.out.println("  6. Follows OPEN/CLOSED PRINCIPLE (add SlackNotifier without modifying OrderService)");
        System.out.println();
        
        // Create notifiers - all implement Notifier interface
        com.lldprep.foundations.oop.interfaces.good.Notifier email = new com.lldprep.foundations.oop.interfaces.good.EmailNotifier("smtp.example.com", 587);
        com.lldprep.foundations.oop.interfaces.good.Notifier sms = new com.lldprep.foundations.oop.interfaces.good.SMSNotifier("api-key-123", "Twilio");
        com.lldprep.foundations.oop.interfaces.good.Notifier push = new com.lldprep.foundations.oop.interfaces.good.PushNotifier("fcm-server-key-456");
        
        // OrderService accepts List<Notifier> - works with ANY Notifier implementation
        com.lldprep.foundations.oop.interfaces.good.OrderService goodService = 
            new com.lldprep.foundations.oop.interfaces.good.OrderService(List.of(email, sms, push));
        
        goodService.placeOrder("user123", "ORD-001", 99.99);
        
        System.out.println("✅ RESULT: Flexible, loosely coupled, easy to maintain\n");
        System.out.println("=".repeat(80) + "\n");
    }
    
    /**
     * Demonstrates FLEXIBILITY: Easy to configure different notification channels.
     */
    private static void demonstrateFlexibility() {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║            BENEFIT #1: FLEXIBILITY                        ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");
        
        System.out.println("Scenario 1: Email-only notifications (VIP customers)");
        com.lldprep.foundations.oop.interfaces.good.Notifier email = new com.lldprep.foundations.oop.interfaces.good.EmailNotifier("smtp.example.com", 587);
        com.lldprep.foundations.oop.interfaces.good.OrderService emailOnlyService = 
            new com.lldprep.foundations.oop.interfaces.good.OrderService(List.of(email));
        emailOnlyService.placeOrder("vip-user", "ORD-002", 499.99);
        
        System.out.println("Scenario 2: SMS + Push notifications (mobile app users)");
        com.lldprep.foundations.oop.interfaces.good.Notifier sms = new com.lldprep.foundations.oop.interfaces.good.SMSNotifier("api-key-123", "Twilio");
        com.lldprep.foundations.oop.interfaces.good.Notifier push = new com.lldprep.foundations.oop.interfaces.good.PushNotifier("fcm-server-key-456");
        com.lldprep.foundations.oop.interfaces.good.OrderService mobileService = 
            new com.lldprep.foundations.oop.interfaces.good.OrderService(List.of(sms, push));
        mobileService.placeOrder("mobile-user", "ORD-003", 29.99);
        
        System.out.println("✅ Same OrderService class, different configurations!");
        System.out.println("   In BAD version, this would require multiple OrderService classes.\n");
        System.out.println("=".repeat(80) + "\n");
    }
    
    /**
     * Demonstrates TESTABILITY: Easy to mock and verify behavior.
     */
    private static void demonstrateTestability() {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║            BENEFIT #2: TESTABILITY                        ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");
        
        System.out.println("Testing with MockNotifier (no real notifications sent):");
        
        com.lldprep.foundations.oop.interfaces.good.MockNotifier mockNotifier = new com.lldprep.foundations.oop.interfaces.good.MockNotifier();
        com.lldprep.foundations.oop.interfaces.good.OrderService testService = 
            new com.lldprep.foundations.oop.interfaces.good.OrderService(List.of(mockNotifier));
        
        testService.placeOrder("test-user", "ORD-TEST", 19.99);
        
        System.out.println("Test Verification:");
        System.out.println("  ✓ Notifications sent: " + mockNotifier.getSendCount());
        System.out.println("  ✓ Captured notifications:");
        for (String notification : mockNotifier.getSentNotifications()) {
            System.out.println("    - " + notification);
        }
        
        System.out.println("\n✅ Testing is trivial with interfaces!");
        System.out.println("   In BAD version, we'd need complex mocking or actually send notifications.\n");
        System.out.println("=".repeat(80) + "\n");
    }
    
    /**
     * Demonstrates EXTENSIBILITY: Adding new notification types without modifying existing code.
     */
    private static void demonstrateExtensibility() {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║       BENEFIT #3: EXTENSIBILITY (Open/Closed)             ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");
        
        System.out.println("Adding Slack notifications (NEW feature):");
        System.out.println("  - Created SlackNotifier class implementing Notifier interface");
        System.out.println("  - ZERO modifications to OrderService");
        System.out.println("  - ZERO modifications to existing notifiers");
        System.out.println("  - Just pass SlackNotifier to OrderService constructor\n");
        
        com.lldprep.foundations.oop.interfaces.good.Notifier email = new com.lldprep.foundations.oop.interfaces.good.EmailNotifier("smtp.example.com", 587);
        com.lldprep.foundations.oop.interfaces.good.Notifier slack = new com.lldprep.foundations.oop.interfaces.good.SlackNotifier("https://hooks.slack.com/xyz", "#orders");
        
        com.lldprep.foundations.oop.interfaces.good.OrderService extendedService = 
            new com.lldprep.foundations.oop.interfaces.good.OrderService(List.of(email, slack));
        
        extendedService.placeOrder("enterprise-user", "ORD-004", 999.99);
        
        System.out.println("✅ Open/Closed Principle in action!");
        System.out.println("   System is OPEN for extension (added Slack)");
        System.out.println("   System is CLOSED for modification (didn't touch existing code)\n");
        System.out.println("   In BAD version, we'd need to:");
        System.out.println("     1. Add private SlackNotifier field to OrderService");
        System.out.println("     2. Modify OrderService constructor");
        System.out.println("     3. Add slackNotifier.sendSlack() call in placeOrder()");
        System.out.println("     4. Recompile and retest OrderService\n");
        System.out.println("=".repeat(80) + "\n");
    }
    
    private static void printKeyTakeaways() {
        System.out.println("\n┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  WHY INTERFACES ARE CRITICAL IN OOP                        │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        System.out.println();
        System.out.println("1. ABSTRACTION");
        System.out.println("   - Define WHAT can be done, hide HOW it's done");
        System.out.println("   - Clients work with contracts, not implementations");
        System.out.println();
        System.out.println("2. POLYMORPHISM");
        System.out.println("   - Treat different implementations uniformly");
        System.out.println("   - Write generic code that works with any implementation");
        System.out.println("   - Example: List<Notifier> works with Email, SMS, Push, Slack, Mock");
        System.out.println();
        System.out.println("3. LOOSE COUPLING (Dependency Inversion Principle)");
        System.out.println("   - Depend on abstractions, not concretions");
        System.out.println("   - High-level modules don't depend on low-level modules");
        System.out.println("   - Both depend on interfaces");
        System.out.println();
        System.out.println("4. FLEXIBILITY");
        System.out.println("   - Swap implementations at runtime");
        System.out.println("   - Configure behavior via dependency injection");
        System.out.println("   - Same code works with different configurations");
        System.out.println();
        System.out.println("5. TESTABILITY");
        System.out.println("   - Easy to create mock/stub implementations");
        System.out.println("   - Test in isolation without real dependencies");
        System.out.println("   - Verify behavior without side effects");
        System.out.println();
        System.out.println("6. EXTENSIBILITY (Open/Closed Principle)");
        System.out.println("   - Add new implementations without modifying existing code");
        System.out.println("   - System is open for extension, closed for modification");
        System.out.println("   - Reduces risk of breaking existing functionality");
        System.out.println();
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  GOLDEN RULE: \"Program to an interface, not an implementation\" │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        System.out.println();
        System.out.println("WITHOUT INTERFACES:");
        System.out.println("  ❌ Tight coupling");
        System.out.println("  ❌ Hard to test");
        System.out.println("  ❌ Difficult to extend");
        System.out.println("  ❌ Violates SOLID principles");
        System.out.println("  ❌ Code duplication");
        System.out.println();
        System.out.println("WITH INTERFACES:");
        System.out.println("  ✅ Loose coupling");
        System.out.println("  ✅ Easy to test");
        System.out.println("  ✅ Simple to extend");
        System.out.println("  ✅ Follows SOLID principles");
        System.out.println("  ✅ Code reuse via polymorphism");
        System.out.println();
    }
}
