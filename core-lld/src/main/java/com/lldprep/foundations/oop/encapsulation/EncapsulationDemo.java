package com.lldprep.foundations.oop.encapsulation;

import com.lldprep.foundations.oop.encapsulation.exception.InsufficientFundsException;

public class EncapsulationDemo {

    public static void main(String[] args) {
        System.out.println("===== ENCAPSULATION =====\n");

        System.out.println("--- Creating account with $500 ---");
        BankAccount account = new BankAccount("Alice", 500.00);
        System.out.println(account);

        System.out.println();
        System.out.println("--- Deposit $200 ---");
        account.deposit(200.00);
        System.out.println("Balance after deposit: $" + account.getBalance());

        System.out.println();
        System.out.println("--- Withdraw $100 ---");
        account.withdraw(100.00);
        System.out.println("Balance after withdrawal: $" + account.getBalance());

        System.out.println();
        System.out.println("--- Attempt overdraft: withdraw $1000 (more than balance) ---");
        try {
            account.withdraw(1000.00);
        } catch (InsufficientFundsException e) {
            System.out.println("Caught InsufficientFundsException: " + e.getMessage());
            System.out.printf("  Requested: $%.2f | Available: $%.2f%n",
                e.getRequestedAmount(), e.getAvailableBalance());
        }
        System.out.println("Balance unchanged after failed withdrawal: $" + account.getBalance());

        System.out.println();
        System.out.println("--- Attempt invalid deposit (negative amount) ---");
        try {
            account.deposit(-50.00);
        } catch (IllegalArgumentException e) {
            System.out.println("Caught IllegalArgumentException: " + e.getMessage());
        }

        System.out.println();
        System.out.println("--- Attempt invalid account creation (negative initial balance) ---");
        try {
            new BankAccount("BadActor", -999.00);
        } catch (IllegalArgumentException e) {
            System.out.println("Caught IllegalArgumentException: " + e.getMessage());
        }

        System.out.println();
        System.out.println("Key invariant upheld: balance is ALWAYS >= 0. No external code can break this.");
        System.out.println("Final state: " + account);

        System.out.println("\n===== END ENCAPSULATION DEMO =====");
    }
}
