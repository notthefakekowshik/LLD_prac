package com.lldprep.foundations.oop.abstractvsinterface;

import java.util.List;

public class AbstractVsInterfaceDemo {

    // Method accepts the Payable INTERFACE — works with any Payable, regardless of hierarchy
    private static void processOrder(Payable processor, double amount) {
        System.out.println("--- Processing order via: " + processor.getPaymentMethod() + " ---");
        processor.pay(amount);
        System.out.println("--- Issuing partial refund ---");
        processor.refund(amount / 2);
        System.out.println();
    }

    public static void main(String[] args) {
        System.out.println("===== ABSTRACT CLASS vs INTERFACE =====\n");

        System.out.println("KEY DISTINCTION:");
        System.out.println("  Interface  = contract (what you CAN do) — no shared implementation");
        System.out.println("  Abstract   = template (what you ARE, with shared behaviour) — forces a family");
        System.out.println();

        CreditCardProcessor cc = new CreditCardProcessor("4242");
        PayPalProcessor pp = new PayPalProcessor("user@example.com");

        // Both are used via the Payable interface reference — polymorphism
        List<Payable> processors = List.of(cc, pp);

        for (Payable p : processors) {
            processOrder(p, 150.00);
        }

        // Demonstrate validation inherited from AbstractPaymentProcessor
        System.out.println("--- Demonstrating shared validation (negative amount) ---");
        try {
            cc.pay(-50.0);
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
        }

        System.out.println();
        System.out.println("Both processors used the SAME validate + log logic from AbstractPaymentProcessor.");
        System.out.println("Neither CreditCardProcessor nor PayPalProcessor duplicated a single line of it.");

        System.out.println("\n===== END ABSTRACT VS INTERFACE DEMO =====");
    }
}
