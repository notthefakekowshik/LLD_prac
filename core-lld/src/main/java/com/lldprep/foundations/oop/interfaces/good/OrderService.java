package com.lldprep.foundations.oop.interfaces.good;

import java.util.List;

/**
 * GOOD CODE: Service class depends on Notifier INTERFACE, not concrete implementations.
 * 
 * BENEFITS OF THIS APPROACH:
 * 
 * 1. LOOSE COUPLING (Dependency Inversion Principle)
 *    - Depends on abstraction (Notifier), not concretions
 *    - Can work with ANY implementation of Notifier
 *    - No knowledge of EmailNotifier, SMSNotifier, etc.
 * 
 * 2. FLEXIBLE
 *    - Want only email? Pass List.of(emailNotifier)
 *    - Want all channels? Pass List.of(email, sms, push, slack)
 *    - Configuration is done at runtime via constructor injection
 * 
 * 3. EASY TO TEST
 *    - Pass MockNotifier in tests
 *    - No real notifications sent during testing
 *    - Can verify notification behavior easily
 * 
 * 4. NO CODE DUPLICATION
 *    - Single loop handles all notifiers
 *    - Generic "notify all channels" logic
 *    - Polymorphism enables uniform treatment
 * 
 * 5. OPEN/CLOSED PRINCIPLE
 *    - Adding SlackNotifier? Just create the class and pass it in
 *    - OrderService code NEVER needs to change
 *    - Open for extension, closed for modification
 * 
 * 6. SINGLE RESPONSIBILITY
 *    - OrderService focuses on order processing
 *    - Notification logic is delegated to Notifier implementations
 *    - Clear separation of concerns
 */
public class OrderService {
    
    private List<Notifier> notifiers;
    
    /**
     * Constructor accepts a LIST of Notifier interfaces.
     * 
     * This is Dependency Injection + Dependency Inversion Principle:
     * - We depend on the Notifier abstraction
     * - Concrete implementations are injected from outside
     * - OrderService has ZERO knowledge of concrete notifier types
     */
    public OrderService(List<Notifier> notifiers) {
        this.notifiers = notifiers;
    }
    
    public void placeOrder(String userId, String orderId, double amount) {
        System.out.println("\n=== Processing Order ===");
        System.out.println("User: " + userId);
        System.out.println("Order ID: " + orderId);
        System.out.println("Amount: $" + amount);
        System.out.println();
        
        // POLYMORPHISM IN ACTION!
        // We can loop over all notifiers because they share the same interface.
        // Each notifier handles the send() call differently, but we don't care HOW.
        
        String message = "Your order " + orderId + " for $" + amount + " has been placed.";
        
        for (Notifier notifier : notifiers) {
            System.out.println("--- Sending via " + notifier.getChannelType() + " ---");
            notifier.send(userId, "Order Confirmation", message);
            System.out.println();
        }
        
        System.out.println("=== Order Processing Complete ===\n");
    }
    
    /**
     * BENEFIT: Want to add Slack notifications?
     * Just pass SlackNotifier in the constructor - NO CODE CHANGES HERE!
     * 
     * Compare this to the BAD version where we'd need to:
     * 1. Add private SlackNotifier field
     * 2. Modify constructor signature
     * 3. Add slackNotifier.sendSlack() call in placeOrder()
     */
    
    /**
     * BENEFIT: Want to send only email for some orders?
     * Just create OrderService with List.of(emailNotifier).
     * The loop automatically handles any number of notifiers (0 to N).
     */
    
    /**
     * BENEFIT: Testing is trivial.
     * Pass MockNotifier and verify it was called:
     * 
     * MockNotifier mock = new MockNotifier();
     * OrderService service = new OrderService(List.of(mock));
     * service.placeOrder("user123", "order456", 99.99);
     * assert mock.getSendCount() == 1;
     */
}
