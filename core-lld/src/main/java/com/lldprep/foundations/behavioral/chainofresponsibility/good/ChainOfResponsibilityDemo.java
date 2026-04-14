package com.lldprep.foundations.behavioral.chainofresponsibility.good;

/**
 * Chain of Responsibility Pattern — Demo
 *
 * <p><b>What problem does it solve?</b><br>
 * Multiple handlers may process a request, but the sender shouldn't need to know which one.
 * Without CoR, the sender must contain hard if-else logic to route requests to the right handler.
 * Adding a new handler requires editing the sender — OCP violation.
 *
 * <p><b>How it works:</b><br>
 * - Each handler knows its threshold and holds a reference to the next handler in the chain.<br>
 * - When a request arrives, the handler processes it if eligible, then passes it to the next handler.<br>
 * - The chain is assembled by the client — handlers are completely independent of each other.<br>
 * - Adding a new handler = new class + one line to insert it in the chain. Zero existing changes.
 *
 * <p><b>Important design note — two styles:</b><br>
 * <ul>
 *   <li><b>Pass-and-process</b> (used here): every eligible handler in the chain processes the
 *       request. An ERROR log is written to console, file, AND email.</li>
 *   <li><b>Stop-on-first</b>: the first matching handler processes it and the chain stops.
 *       Used in authentication pipelines, approval workflows.</li>
 * </ul>
 *
 * <p><b>When to use:</b>
 * <ul>
 *   <li>Multiple handlers may process a request, and the handler isn't known until runtime.</li>
 *   <li>You want to avoid hardcoded routing logic in the sender.</li>
 *   <li>Building middleware pipelines, log handlers, approval workflows, auth filters.</li>
 * </ul>
 *
 * <p><b>Gotcha:</b> CoR has no guarantee a request is handled — always add a default/fallback
 * handler at the end of the chain for unhandled cases.
 *
 * <p><b>Covered variations:</b>
 * <ol>
 *   <li>Pass-and-process (all eligible handlers run)</li>
 *   <li>Chain reconfiguration at runtime (different chain for prod vs dev)</li>
 *   <li>Evolve: adding a new handler without touching existing ones</li>
 * </ol>
 */
public class ChainOfResponsibilityDemo {

    public static void main(String[] args) {
        demo1_LogChain();
        demo2_RuntimeChainReconfiguration();
        demo3_EvolveAddNewHandler();
    }

    // -------------------------------------------------------------------------

    private static void demo1_LogChain() {
        section("Demo 1: Log chain — each level triggers a different set of handlers");

        LogHandler chain = buildProductionChain();

        System.out.println("\n  -- Sending DEBUG --");
        chain.handle(LogLevel.DEBUG, "Cache miss for key 'user:42'");

        System.out.println("\n  -- Sending WARN --");
        chain.handle(LogLevel.WARN, "Response time > 2s on /checkout");

        System.out.println("\n  -- Sending ERROR --");
        chain.handle(LogLevel.ERROR, "Payment service unreachable");
    }

    private static void demo2_RuntimeChainReconfiguration() {
        section("Demo 2: Dev chain (no email alerts — only console output)");

        // Dev environment: only console, no file/email noise
        LogHandler devChain = new ConsoleHandler();

        devChain.handle(LogLevel.ERROR, "NullPointerException in UserService");
        // Only console — no file write, no email sent
    }

    private static void demo3_EvolveAddNewHandler() {
        section("Demo 3: Evolve — add SMSHandler for ERROR without touching existing handlers");

        LogHandler console = new ConsoleHandler();
        LogHandler file    = new FileHandler();
        LogHandler email   = new EmailAlertHandler("oncall@company.com");
        LogHandler sms     = new SMSHandler("+91-9999999999"); // NEW handler

        // Insert SMS into the chain — zero changes to other handlers
        console.setNext(file).setNext(email).setNext(sms);

        System.out.println();
        console.handle(LogLevel.ERROR, "Database connection pool exhausted");
    }

    // -------------------------------------------------------------------------

    private static LogHandler buildProductionChain() {
        LogHandler console = new ConsoleHandler();
        LogHandler file    = new FileHandler();
        LogHandler email   = new EmailAlertHandler("oncall@company.com");

        // Fluent chain assembly: console → file → email
        console.setNext(file).setNext(email);
        return console; // return head of chain
    }

    private static void section(String title) {
        System.out.println("\n=== " + title + " ===");
    }

    // -------------------------------------------------------------------------
    // Evolve step: new handler added without touching any existing class
    // -------------------------------------------------------------------------

    static class SMSHandler extends LogHandler {
        private final String phoneNumber;

        SMSHandler(String phoneNumber) {
            super(LogLevel.ERROR);
            this.phoneNumber = phoneNumber;
        }

        @Override
        protected void write(LogLevel level, String message) {
            System.out.println("  [SMS    ][" + level + "] Texting " + phoneNumber + ": " + message);
        }
    }
}
