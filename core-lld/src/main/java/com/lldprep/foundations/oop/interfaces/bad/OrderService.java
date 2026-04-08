package com.lldprep.foundations.oop.interfaces.bad;

/**
 * BAD CODE: Service class tightly coupled to concrete notification implementations.
 * 
 * PROBLEMS WITH THIS APPROACH:
 * 
 * 1. TIGHT COUPLING
 *    - OrderService directly depends on EmailNotifier, SMSNotifier, PushNotifier
 *    - Cannot swap implementations without modifying OrderService
 *    - Violates Dependency Inversion Principle (DIP)
 * 
 * 2. INFLEXIBLE
 *    - Want to add Slack notifications? Must modify OrderService constructor
 *    - Want to use only email? Still forced to create all three notifiers
 *    - Cannot configure notification channels at runtime
 * 
 * 3. HARD TO TEST
 *    - Cannot mock notifiers easily
 *    - Tests will actually send notifications (or require complex setup)
 *    - Cannot verify notification behavior without running real implementations
 * 
 * 4. CODE DUPLICATION
 *    - Each notification type requires separate method calls
 *    - Cannot loop over notifiers
 *    - Cannot write generic "notify all channels" logic
 * 
 * 5. VIOLATES OPEN/CLOSED PRINCIPLE
 *    - Adding new notification type requires modifying this class
 *    - Not open for extension, requires modification
 */
public class OrderService {
    
    private EmailNotifier emailNotifier;
    private SMSNotifier smsNotifier;
    private PushNotifier pushNotifier;
    
    // Constructor is FORCED to take all three concrete types
    public OrderService(EmailNotifier emailNotifier, 
                       SMSNotifier smsNotifier, 
                       PushNotifier pushNotifier) {
        this.emailNotifier = emailNotifier;
        this.smsNotifier = smsNotifier;
        this.pushNotifier = pushNotifier;
    }
    
    public void placeOrder(String userId, String orderId, double amount) {
        System.out.println("\n=== Processing Order ===");
        System.out.println("User: " + userId);
        System.out.println("Order ID: " + orderId);
        System.out.println("Amount: $" + amount);
        System.out.println();
        
        // PROBLEM: Each notifier has a DIFFERENT method signature
        // We cannot write a loop or generic code here
        
        // Email notification
        emailNotifier.sendEmail(
            userId + "@example.com",
            "Order Confirmation",
            "Your order " + orderId + " for $" + amount + " has been placed."
        );
        
        System.out.println();
        
        // SMS notification - DIFFERENT method name and parameters
        smsNotifier.sendSMS(
            "+1234567890",
            "Order " + orderId + " confirmed. Amount: $" + amount
        );
        
        System.out.println();
        
        // Push notification - YET ANOTHER different method signature
        pushNotifier.sendPushNotification(
            "device-token-xyz",
            "Order Placed",
            "Your order has been confirmed!",
            "{\"orderId\":\"" + orderId + "\",\"amount\":" + amount + "}"
        );
        
        System.out.println("\n=== Order Processing Complete ===\n");
    }
    
    /**
     * PROBLEM: What if we want to add Slack notifications?
     * We would need to:
     * 1. Add a new field: private SlackNotifier slackNotifier
     * 2. Modify the constructor to accept SlackNotifier
     * 3. Add another method call in placeOrder()
     * 
     * This violates the Open/Closed Principle!
     */
    
    /**
     * PROBLEM: What if we want to send only email notifications for some orders?
     * We cannot - all three notifiers are hardcoded and will always be called.
     */
    
    /**
     * PROBLEM: How do we test this without actually sending emails/SMS/push?
     * Very difficult - we'd need to mock three different concrete classes.
     */
}
