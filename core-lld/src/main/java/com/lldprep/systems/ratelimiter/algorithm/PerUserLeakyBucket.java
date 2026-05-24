package com.lldprep.systems.ratelimiter.algorithm;

import com.lldprep.systems.ratelimiter.AlgorithmType;
import com.lldprep.systems.ratelimiter.RateLimitConfig;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Leaky Bucket — single-threaded, no locks.
 *
 * Requests enter a queue and "leak" (drain) at a constant rate. If the queue is
 * full when a new request arrives, it is denied. Unlike Token Bucket, there is no
 * concept of "saved up" capacity — even a single burst will be smoothed.
 *
 * Analogy: water poured quickly into a bucket with a small hole at the bottom.
 * The water level (queue) rises during bursts; the hole (leak rate) ensures a
 * steady drip out regardless of how fast water comes in.
 *
 * Use when: downstream systems need a steady, predictable flow (e.g., DB writes,
 * message queues, background job processors where spikes cause cascading failures).
 */
public class PerUserLeakyBucket implements PerUserAlgorithm {

    private final int capacity;
    private final double leakRatePerNano;
    private final Queue<Long> queue;
    private long lastLeakTime;

    public PerUserLeakyBucket(RateLimitConfig config) {
        this.capacity = config.getBurstSize();
        this.leakRatePerNano = config.getLeakRatePerNano();
        this.queue = new LinkedList<>();
        this.lastLeakTime = System.nanoTime();
    }

    @Override
    public boolean tryConsume(int permits) {
        leak();
        if (queue.size() + permits <= capacity) {
            long now = System.nanoTime();
            for (int i = 0; i < permits; i++) {
                queue.offer(now);
            }
            return true;
        }
        return false;
    }

    @Override
    public long availablePermits() {
        leak();
        return capacity - queue.size();
    }

    @Override
    public void reset() {
        queue.clear();
        lastLeakTime = System.nanoTime();
    }

    @Override
    public AlgorithmType getAlgorithmType() {
        return AlgorithmType.LEAKY_BUCKET;
    }

    private void leak() {
        long now = System.nanoTime();
        long elapsed = now - lastLeakTime;
        if (elapsed <= 0) return;
        int count = (int) Math.min(elapsed * leakRatePerNano, queue.size());
        for (int i = 0; i < count; i++) {
            queue.poll();
        }
        lastLeakTime = now;
    }
}
