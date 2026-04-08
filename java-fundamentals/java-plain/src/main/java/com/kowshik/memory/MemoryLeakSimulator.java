package com.kowshik.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MemoryLeakSimulator {

    // THE LEAK: A static list acts as a GC Root.
    // Objects added here can NEVER be collected.
    private static final List<byte[]> LEAKY_CACHE = new ArrayList<>();
    private static final Random random = new Random();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("App Started. PID: " + ProcessHandle.current().pid());
        System.out.println("Simulating a memory leak...");

        int iterations = 0;

        while (true) {
            // Allocate 1MB of memory (Data Block)
            byte[] dataChunk = new byte[1024 * 1024];

            // Fill it with random data so the JVM doesn't optimize it away
            random.nextBytes(dataChunk);

            // Add to the static list (Strong Reference)
            LEAKY_CACHE.add(dataChunk);

            iterations++;

            // Log every 50MB so we can see the progression
            if (iterations % 50 == 0) {
                System.out.println("Cache size: " + iterations + " MB");
                printMemoryStats();
            }

            // Sleep slightly to mimic real traffic and give you time to attach tools
            Thread.sleep(50);
        }
    }

    private static void printMemoryStats() {
        long usedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println("Current Heap Used: " + (usedMem / 1024 / 1024) + " MB");
    }
}