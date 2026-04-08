package com.kowshik.lambdas.functional_interfaces;

import java.util.List;
import java.util.function.Consumer;

/**
 * Consumer<T> (Action)
 * Method: void accept(T t)
 * Signature: T -> void
 * Purpose: Performs an action on the input (e.g., side-effects like logging, saving).
 */
public class ConsumerDemo {
    public static void main(String[] args) {
        List<String> logs = List.of("Starting server...", "Request received", "DB updated", "Process finished");

        // 1. Basic Consumer (Method Reference)
        Consumer<String> simpleLog = System.out::println;
        logs.forEach(simpleLog);

        // 2. Chaining Consumers (andThen)
        Consumer<String> upperCaseLog = log -> System.out.println("[AUDIT]: " + log.toUpperCase());
        Consumer<String> fileSimulator = log -> System.out.println("[FILE_SYSTEM]: Appending log: " + log);

        System.out.println("\n=== Multi-Action Logging (Audit + File System) ===");
        Consumer<String> multiAction = upperCaseLog.andThen(fileSimulator);
        
        logs.forEach(multiAction);
    }
}
