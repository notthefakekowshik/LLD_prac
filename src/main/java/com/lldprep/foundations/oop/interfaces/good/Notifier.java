package com.lldprep.foundations.oop.interfaces.good;

/**
 * GOOD CODE: Interface defines a CONTRACT for notification behavior.
 * 
 * Benefits of this interface:
 * 
 * 1. ABSTRACTION
 *    - Defines WHAT can be done (send notifications)
 *    - Hides HOW it's done (implementation details)
 * 
 * 2. POLYMORPHISM
 *    - Any class implementing this can be used interchangeably
 *    - Code can work with Notifier reference, not concrete types
 * 
 * 3. LOOSE COUPLING
 *    - Clients depend on this interface, not concrete implementations
 *    - Implementations can be swapped without changing client code
 * 
 * 4. TESTABILITY
 *    - Easy to create mock/stub implementations for testing
 *    - Can verify behavior without real email/SMS/push infrastructure
 * 
 * 5. EXTENSIBILITY (Open/Closed Principle)
 *    - New notification types = new implementations
 *    - No need to modify existing code
 */
public interface Notifier {
    
    /**
     * Sends a notification to the specified recipient.
     * 
     * This is a UNIFIED contract - all notifiers must implement this method.
     * The implementation details (email vs SMS vs push) are hidden.
     * 
     * @param recipient The recipient identifier (email, phone, device token, etc.)
     * @param subject   The notification subject/title
     * @param message   The notification message body
     */
    void send(String recipient, String subject, String message);
    
    /**
     * Returns the type of notification channel.
     * Useful for logging and debugging.
     * 
     * @return Notification channel type (e.g., "EMAIL", "SMS", "PUSH")
     */
    String getChannelType();
}
