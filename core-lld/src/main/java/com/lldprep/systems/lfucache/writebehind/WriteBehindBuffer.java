package com.lldprep.systems.lfucache.writebehind;

import com.lldprep.systems.lfucache.model.WriteBehindTask;
import com.lldprep.systems.lfucache.store.BackingStore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Write-behind buffer: accepts writes from LFUCache immediately (non-blocking),
 * then flushes them to BackingStore asynchronously in batches.
 *
 * Why write-behind over write-through:
 *   write-through: cache.put() blocks until DB write completes  — low write latency risk
 *   write-behind:  cache.put() returns immediately; DB write is async — higher throughput,
 *                  but window of data loss if the process dies before flush.
 *
 * Trade-off accepted here: suitable for write-heavy workloads (analytics, counters, feeds)
 * where eventual persistence is acceptable. NOT suitable for financial transactions.
 */
public class WriteBehindBuffer<K, V> {

    private final BlockingQueue<WriteBehindTask<K, V>> queue;
    private final BackingStore<K, V> backingStore;
    private final int batchSize;
    private volatile boolean running = true;
    private final Thread flusherThread;

    public WriteBehindBuffer(BackingStore<K, V> backingStore, int batchSize) {
        this.backingStore = backingStore;
        this.batchSize = batchSize;
        this.queue = new LinkedBlockingQueue<>();
        this.flusherThread = new Thread(this::flushLoop, "write-behind-flusher");
        this.flusherThread.setDaemon(true);
        this.flusherThread.start();
    }

    /** Non-blocking enqueue. Called by LFUCache.put() on the hot path. */
    public void enqueue(K key, V value) {
        queue.offer(new WriteBehindTask<>(key, value));
    }

    private void flushLoop() {
        while (running || !queue.isEmpty()) {
            try {
                // Block up to 100ms for first item; then drain up to batchSize in one go
                WriteBehindTask<K, V> head = queue.poll(100, TimeUnit.MILLISECONDS);
                if (head == null) continue;

                List<WriteBehindTask<K, V>> batch = new ArrayList<>(batchSize);
                batch.add(head);
                queue.drainTo(batch, batchSize - 1);

                for (WriteBehindTask<K, V> task : batch) {
                    backingStore.write(task.key(), task.value());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /** Signals the flusher to stop after draining remaining items. */
    public void shutdown() {
        running = false;
        flusherThread.interrupt();
    }
}
