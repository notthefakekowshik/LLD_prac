package com.kowshik.coordination;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Exchanger;

/**
 * ExchangerDemo — Pairwise Thread Data Handoff
 *
 * INTERVIEW PREP:
 * ==============
 * Q: What is Exchanger used for?
 * A: A synchronization point where exactly TWO threads swap objects.
 *    Both threads block until the other arrives, then each gets the other's object.
 *
 * Q: What happens if only one thread calls exchange() and the other never does?
 * A: The first thread blocks forever (or until timeout, if exchange(v, timeout, unit) is used).
 *
 * Q: Real use case?
 * A: Double-buffering pipeline — producer fills a buffer while consumer drains the
 *    previous one. They swap at each cycle with zero copying.
 */
public class ExchangerDemo {

    // ─────────────────────────────────────────────────────────────────────────
    // Part 1: Basic exchange — producer/consumer buffer swap
    // ─────────────────────────────────────────────────────────────────────────
    static void basicExchange() throws InterruptedException {
        System.out.println("--- Part 1: Basic Buffer Exchange ---");

        Exchanger<List<Integer>> exchanger = new Exchanger<>();

        // Producer: fills a buffer and swaps for an empty one
        Thread producer = new Thread(() -> {
            List<Integer> buffer = new ArrayList<>();
            for (int cycle = 0; cycle < 3; cycle++) {
                // Fill buffer
                for (int i = 0; i < 5; i++) {
                    int item = cycle * 5 + i;
                    buffer.add(item);
                }
                System.out.printf("[Producer] filled buffer: %s (cycle %d)%n", buffer, cycle);

                try {
                    // Hand off full buffer, receive empty buffer
                    buffer = exchanger.exchange(buffer);
                    System.out.printf("[Producer] received empty buffer (cycle %d)%n", cycle);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "producer");

        // Consumer: drains the buffer and swaps for a new full one
        Thread consumer = new Thread(() -> {
            List<Integer> buffer = new ArrayList<>(); // Start with empty buffer
            for (int cycle = 0; cycle < 3; cycle++) {
                try {
                    // Hand off empty buffer, receive full buffer
                    buffer = exchanger.exchange(buffer);
                    System.out.printf("[Consumer] received full buffer: %s (cycle %d)%n", buffer, cycle);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                // Drain the buffer
                buffer.clear();
                System.out.printf("[Consumer] drained buffer (cycle %d)%n", cycle);
            }
        }, "consumer");

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Part 2: Exchange with timeout — avoid blocking forever
    // ─────────────────────────────────────────────────────────────────────────
    static void exchangeWithTimeout() throws InterruptedException {
        System.out.println("\n--- Part 2: Exchange with Timeout ---");

        Exchanger<String> exchanger = new Exchanger<>();

        Thread t1 = new Thread(() -> {
            try {
                System.out.println("[T1] offering 'hello', waiting for partner...");
                String received = exchanger.exchange("hello", 1, java.util.concurrent.TimeUnit.SECONDS);
                System.out.println("[T1] received: " + received);
            } catch (java.util.concurrent.TimeoutException e) {
                System.out.println("[T1] timeout — no partner arrived in 1 second");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                Thread.sleep(500); // Arrives within timeout
                System.out.println("[T2] offering 'world'...");
                String received = exchanger.exchange("world");
                System.out.println("[T2] received: " + received);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Exchanger Demo ===\n");

        basicExchange();
        exchangeWithTimeout();

        System.out.println("\n=== Done ===");
    }
}
