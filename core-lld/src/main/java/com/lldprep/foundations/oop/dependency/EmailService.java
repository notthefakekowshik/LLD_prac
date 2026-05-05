// DEPENDENCY: EmailService is used transiently by OrderService.
// It is NOT stored as a field — just passed as a method argument.
// OrderService depends on EmailService only for the duration of a method call.
package com.lldprep.foundations.oop.dependency;

public class EmailService {

    public void sendConfirmation(String recipient, String orderId) {
        System.out.println("Email sent to " + recipient + " for order [" + orderId + "]");
    }
}
