package com.lldprep.foundations.solid.dip;

import com.lldprep.foundations.solid.dip.bad.UserService;
import com.lldprep.foundations.solid.dip.good.InMemoryUserRepository;
import com.lldprep.foundations.solid.dip.good.MySQLUserRepository;

public class DIPDemo {

    public static void main(String[] args) {
        System.out.println("===== DIP: DEPENDENCY INVERSION PRINCIPLE =====\n");

        // --- BAD VERSION ---
        System.out.println("--- BAD: UserService is hard-wired to MySQLUserRepository ---");
        System.out.println("Problem: Cannot swap to InMemory without editing UserService.");
        UserService badService = new UserService(); // internally creates MySQL — no choice
        badService.registerUser("alice");

        System.out.println();

        // --- GOOD VERSION: with MySQL ---
        System.out.println("--- GOOD: Inject MySQLUserRepository ---");
        com.lldprep.foundations.solid.dip.good.UserService mysqlService =
            new com.lldprep.foundations.solid.dip.good.UserService(new MySQLUserRepository());
        mysqlService.registerUser("bob");

        System.out.println();

        // --- GOOD VERSION: swapped to InMemory with ONE line change ---
        System.out.println("--- GOOD: Swap to InMemoryUserRepository — UserService unchanged ---");
        com.lldprep.foundations.solid.dip.good.UserService inMemoryService =
            new com.lldprep.foundations.solid.dip.good.UserService(new InMemoryUserRepository());
        inMemoryService.registerUser("charlie");

        System.out.println("\nKey insight: UserService code is IDENTICAL in both good cases.");
        System.out.println("The CALLER decides the implementation — UserService never knows which one.");

        System.out.println("\n===== END DIP DEMO =====");
    }
}
