// DEPENDENCY: OrderService USES EmailService as a method parameter.
// No field — EmailService is NOT stored inside OrderService.
// This is the loosest coupling: if EmailService changes, OrderService
// only needs a recompile if the method signature changes.
// Relationship: OrderService - - -> EmailService  (dashed arrow = uses/depends-on)
package com.lldprep.foundations.oop.dependency;

public class OrderService {

    public void placeOrder(String customerId, String orderId, EmailService emailService) {
        System.out.println("Order [" + orderId + "] placed for customer [" + customerId + "]");
        // Uses EmailService transiently — it is a dependency, not an association
        emailService.sendConfirmation(customerId, orderId);
    }
}
