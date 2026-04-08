// INTERFACE: Defines the public contract — what any payment processor must be able to do.
// Interfaces define CAPABILITIES. Any class can implement Payable regardless of its hierarchy.
// Multiple classes from different hierarchies can all be treated as Payable.
package com.lldprep.foundations.oop.abstractvsinterface;

public interface Payable {
    void pay(double amount);
    void refund(double amount);
    String getPaymentMethod();
}
