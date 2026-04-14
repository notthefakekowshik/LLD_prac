package com.lldprep.foundations.behavioral.chainofresponsibility.bad;

/**
 * BAD: A single class handles all log levels with a hard if-else chain.
 *
 * Problems:
 * 1. OCP violation — adding a new level (TRACE, FATAL) requires editing this class.
 * 2. All routing logic is centralized — the class must know about every handler upfront.
 * 3. Cannot reorder, add, or remove handlers at runtime.
 * 4. Cannot plug in different handlers for different deployments (prod vs dev).
 */
public class LoggerBad {

    public void log(int level, String message) {
        if (level == 1) {
            System.out.println("[DEBUG] " + message);
        } else if (level == 2) {
            System.out.println("[INFO]  " + message);
        } else if (level == 3) {
            System.out.println("[WARN]  " + message);
        } else if (level == 4) {
            System.out.println("[ERROR] " + message);
            // also send alert email — now coupled to email logic
        }
        // Adding FATAL: edit this method again. OCP violation.
    }
}
