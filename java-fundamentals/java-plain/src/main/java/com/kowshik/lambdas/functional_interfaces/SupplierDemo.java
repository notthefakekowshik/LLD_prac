package com.kowshik.lambdas.functional_interfaces;

import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * Supplier<T> (Factory/Producer)
 * Method: T get()
 * Signature: () -> T
 * Purpose: Produces an object without taking any input. Often used for lazy generation or providing default values.
 */
public class SupplierDemo {
    public static void main(String[] args) {
        
        // 1. Basic Supplier (Data generation)
        Supplier<LocalDateTime> timeNow = LocalDateTime::now;
        System.out.println("Current time: " + timeNow.get());

        // 2. Lazy Initialization (Simulation)
        Supplier<String> lazyString = () -> {
            System.out.println("Expensive call: loading string from database...");
            return "This string was loaded lazily.";
        };

        boolean condition = true; // imagine this comes from user logic

        if (condition) {
            // Only now the code inside the lambda is executed
            System.out.println(lazyString.get());
        } else {
            System.out.println("Condition was false, no expensive call made.");
        }

        // 3. Generating default values
        Supplier<Double> defaultAmount = () -> 0.0;
        System.out.println("Default amount: " + defaultAmount.get());
    }
}
