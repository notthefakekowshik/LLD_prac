package com.kowshik.generics;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates why Generics were introduced in Java 5.
 * 1. Stronger type checks at compile time.
 * 2. Elimination of explicit casts.
 */
public class WhyGenericsDemo {

    public static void main(String[] args) {
        System.out.println("--- Before Generics (Java 1.4 and earlier) ---");
        List oldList = new ArrayList();
        oldList.add("Hello");
        oldList.add(10); // Compiler allows adding an Integer to what we might intend as a String list.

        // When retrieving, we don't know the exact type, so we get Objects.
        // We have to explicitly cast them.
        String str1 = (String) oldList.get(0);
        System.out.println("Retrieved: " + str1);

        try {
            // This will cause a ClassCastException at RUNTIME because the 2nd element is an
            // Integer.
            String str2 = (String) oldList.get(1);
        } catch (ClassCastException e) {
            System.out.println("Runtime Exception: " + e.getMessage());
        }

        System.out.println("\n--- After Generics (Java 5+) ---");
        // We specify the type of elements the list will hold using angle brackets <>.
        List<String> genericList = new ArrayList<>();
        genericList.add("Hello");
        // genericList.add(10); // COMPILE TIME ERROR! We catch the bug early.

        // No casting is required because the compiler knows the list only contains
        // Strings.
        String str3 = genericList.get(0);
        System.out.println("Retrieved easily: " + str3);
    }
}
