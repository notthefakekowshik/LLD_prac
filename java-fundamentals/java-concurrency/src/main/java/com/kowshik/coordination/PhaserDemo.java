package com.kowshik.coordination;

import java.util.concurrent.Phaser;

/**
 * PhaserDemo — Dynamic Party Registration, Multi-Phase Computation, Termination
 *
 * INTERVIEW PREP:
 * ==============
 * Q: When would you use Phaser over CyclicBarrier?
 * A: When the number of participating threads changes between phases (dynamic register/
 *    deregister), when you need termination logic (onAdvance), or when you need
 *    tiered synchronization (tree of Phasers).
 *
 * Q: What is arriveAndAwaitAdvance vs arriveAndDeregister?
 * A: arriveAndAwaitAdvance: signal arrival AND wait for all others → typical barrier call.
 *    arriveAndDeregister: signal arrival AND remove yourself from future phases → done.
 *
 * Q: Can Phaser replace CountDownLatch and CyclicBarrier?
 * A: Yes — it's a superset. But prefer CountDownLatch/CyclicBarrier for fixed, simple
 *    scenarios. Phaser's API is more complex and should be used only when its extra
 *    features (dynamic parties, termination) are needed.
 */
public class PhaserDemo {

    // ─────────────────────────────────────────────────────────────────────────
    // Part 1: Basic Phaser — replace CyclicBarrier (fixed parties, 2 phases)
    // ─────────────────────────────────────────────────────────────────────────
    static void basicPhaser() throws InterruptedException {
        System.out.println("--- Part 1: Basic Phaser (2 phases, 3 workers) ---");

        int workerCount = 3;
        // Register the main thread + workers. Main thread deregisters after starting workers.
        Phaser phaser = new Phaser(1 + workerCount); // 1 for main, 3 for workers

        for (int i = 0; i < workerCount; i++) {
            final int id = i;
            new Thread(() -> {
                System.out.printf("  [Worker-%d] Phase 0: processing...%n", id);
                try { Thread.sleep(100 + id * 50); } catch (InterruptedException e) { return; }

                phaser.arriveAndAwaitAdvance(); // All workers sync at phase 0 → 1

                System.out.printf("  [Worker-%d] Phase 1: processing...%n", id);
                try { Thread.sleep(80 + id * 30); } catch (InterruptedException e) { return; }

                phaser.arriveAndDeregister(); // Done — remove from future phases
            }, "Worker-" + i).start();
        }

        phaser.arriveAndDeregister(); // Main deregisters itself
        // Wait for all phases to complete:
        while (!phaser.isTerminated()) {
            Thread.sleep(50);
        }
        System.out.println("  All phases complete. Phaser terminated: " + phaser.isTerminated());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Part 2: Dynamic Party Registration — threads join and leave mid-way
    // ─────────────────────────────────────────────────────────────────────────
    static void dynamicParties() throws InterruptedException {
        System.out.println("\n--- Part 2: Dynamic Party Registration ---");

        Phaser phaser = new Phaser(1); // Start with just main thread

        // Spawn workers dynamically, each registers itself
        for (int i = 0; i < 4; i++) {
            final int id = i;
            phaser.register(); // Add this worker as a party BEFORE starting it
            new Thread(() -> {
                System.out.printf("  [Dynamic-%d] registered, arrived at phase 0%n", id);
                phaser.arriveAndDeregister(); // Arrive at phase 0 and deregister
            }, "Dynamic-" + i).start();
            Thread.sleep(20); // Stagger arrivals
        }

        phaser.arriveAndDeregister(); // Main arrives and deregisters
        System.out.println("  Phase 0 completed with dynamic parties");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Part 3: Termination via onAdvance override
    // Phaser terminates automatically after N phases
    // ─────────────────────────────────────────────────────────────────────────
    static void terminationDemo() throws InterruptedException {
        System.out.println("\n--- Part 3: Auto-termination after 3 phases ---");

        int maxPhases = 3;
        Phaser phaser = new Phaser(3) { // 3 workers
            @Override
            protected boolean onAdvance(int phase, int registeredParties) {
                System.out.printf("  [Phaser] Phase %d complete. Parties: %d%n",
                        phase, registeredParties);
                // Terminate when all phases done or no parties left
                return phase >= maxPhases - 1 || registeredParties == 0;
            }
        };

        for (int i = 0; i < 3; i++) {
            final int id = i;
            new Thread(() -> {
                while (!phaser.isTerminated()) {
                    int phase = phaser.getPhase();
                    // Do work for current phase
                    try { Thread.sleep(50); } catch (InterruptedException e) { return; }
                    System.out.printf("  [T%d] arrived at phase %d%n", id, phase);
                    phaser.arriveAndAwaitAdvance();
                }
                System.out.printf("  [T%d] phaser terminated, exiting%n", id);
            }, "T" + i).start();
        }

        // Wait for all threads to complete all phases
        Thread.sleep(800);
        System.out.println("  Phaser terminated: " + phaser.isTerminated());
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Phaser Demo ===\n");

        basicPhaser();
        dynamicParties();
        terminationDemo();

        System.out.println("\n=== Done ===");
    }
}
