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

    /**
     * Continuous batch-drain loop running in a daemon thread.
     *
     * BlockingQueue methods used in this method:
     *
     *   poll(timeout, unit)
     *     Waits up to the given time for an element. Returns null if the timeout
     *     elapses with no element available. Non-blocking beyond the timeout.
     *     We use it (over take()) so the loop can periodically check `running`
     *     and exit during shutdown instead of blocking forever on an empty queue.
     *
     *   drainTo(collection, maxElements)
     *     Atomically removes up to maxElements from the queue and adds them to
     *     the given collection — all under a SINGLE lock acquisition. Much more
     *     efficient than calling poll() in a loop (which would acquire and release
     *     the lock N times). After the first poll() confirms there's work, drainTo
     *     scoops up any additional items that arrived between the poll and now.
     *
     * Flow:
     *   - Block up to 100ms waiting for the first item.
     *   - If timeout elapses with no item → loop back (check running flag).
     *   - If item arrives → drain up to batchSize items, flush all to BackingStore.
     *   - On InterruptedException (shutdown signal) → exit loop.
     */
    private void flushLoop() {
        while (running || !queue.isEmpty()) {
            try {
                WriteBehindTask<K, V> head = queue.poll(100, TimeUnit.MILLISECONDS);
                if (head == null) {
                    continue;
                }

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
