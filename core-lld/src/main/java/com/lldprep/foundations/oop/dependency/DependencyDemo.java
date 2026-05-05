package com.lldprep.foundations.oop.dependency;

public class DependencyDemo {

    public static void main(String[] args) {
        System.out.println("===== DEPENDENCY =====\n");

        OrderService orderService = new OrderService();
        EmailService emailService = new EmailService();

        // EmailService is passed as a method argument — NOT stored inside OrderService
        orderService.placeOrder("customer-42", "ORD-001", emailService);

        System.out.println();
        System.out.println("--- Another call, same OrderService, same EmailService ---");
        orderService.placeOrder("customer-99", "ORD-002", emailService);

        System.out.println();
        System.out.println("Key insight: OrderService has NO reference to EmailService as a field.");
        System.out.println("EmailService is only alive for the duration of placeOrder().");
        System.out.println("You can swap to SMSService or MockEmailService without changing OrderService's constructor.");
        System.out.println("Relationship type: OrderService - - -> EmailService  (uses, transient)");
        System.out.println("\n===== END DEPENDENCY DEMO =====");
    }
}
