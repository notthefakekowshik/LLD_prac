// ENCAPSULATION: balance invariant (never negative) is enforced here — callers cannot break it.
// Private fields prevent external code from setting balance = -1000 directly.
// All mutation goes through validated methods, ensuring the class's invariants always hold.
// Thread-safety is achieved via synchronized methods — balance stays consistent under concurrent access.
package com.lldprep.foundations.oop.encapsulation;

import com.lldprep.foundations.oop.encapsulation.exception.InsufficientFundsException;

public class BankAccount {

    // ENCAPSULATION: fields are private — outside world has no direct access
    private final String owner;
    private double balance; // invariant: balance >= 0 at all times

    public BankAccount(String owner, double initialBalance) {
        if (initialBalance < 0) {
            throw new IllegalArgumentException(
                "Initial balance cannot be negative. Got: " + initialBalance);
        }
        this.owner = owner;
        this.balance = initialBalance;
    }

    // Read-only access — callers can observe balance but cannot set it arbitrarily
    public String getOwner() {
        return owner;
    }

    public synchronized double getBalance() {
        return balance;
    }

    // Validated mutation — deposit is the ONLY way to increase balance from outside
    public synchronized void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException(
                "Deposit amount must be positive. Got: " + amount);
        }
        balance += amount;
        System.out.printf("[%s] Deposited $%.2f | New balance: $%.2f%n", owner, amount, balance);
    }

    // Validated mutation — invariant enforced: balance never goes below 0
    public synchronized void withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException(
                "Withdrawal amount must be positive. Got: " + amount);
        }
        if (amount > balance) {
            throw new InsufficientFundsException(amount, balance);
        }
        balance -= amount;
        System.out.printf("[%s] Withdrew  $%.2f | New balance: $%.2f%n", owner, amount, balance);
    }

    @Override
    public String toString() {
        return String.format("BankAccount{owner='%s', balance=$%.2f}", owner, balance);
    }
}
