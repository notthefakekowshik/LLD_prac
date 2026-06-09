package com.lldprep.systems.lfucache.policy;

import java.util.HashMap;
import java.util.Map;

/**
 * O(1) LFU eviction with TRUE LRU tie-breaking within each frequency bucket.
 *
 * This is the textbook "paper" implementation (Shah, Mitra, Matani 2010).
 * Unlike O1LFUEvictionPolicy (which delegates to LinkedHashSet and gets
 * insertion-order / FIFO within a bucket), this version hand-rolls a
 * doubly-linked-list per frequency so that the head of each list is ALWAYS
 * the least-recently-accessed key at that frequency.
 *
 * ── WHY THREE DATA STRUCTURES ──────────────────────────────────────────
 *
 *   keyNode  : Map<K, Node<K>>
 *       Maps every key to its Node wrapper so we can remove it in O(1)
 *       from its current frequency list. The Node stores its own frequency.
 *
 *   freqList : Map<Integer, DoublyLinkedList<K>>
 *       One DLL per frequency. Each list is ordered by last-access-time:
 *         HEAD = least-recently accessed at this frequency.
 *         TAIL = most-recently  accessed at this frequency.
 *       Eviction always pops the HEAD of the lowest occupied frequency.
 *
 *   minFreq  : int
 *       Tracks the lowest frequency that currently has at least one key.
 *       Without this we'd scan from 1 upward on eviction → O(n) worst case.
 *
 * ── WHY DOUBLY-LINKED LIST ─────────────────────────────────────────────
 *
 *   We need these operations, all O(1):
 *     1. remove(Node)         — unlink from middle of list (when key is accessed
 *                               and promoted to freq+1). Singly-linked can't
 *                               do this without scanning from the head.
 *     2. addToTail(Node)      — append after access so the key becomes MRU
 *                               in its new frequency bucket.
 *     3. removeHead()         — evict the LRU key within a bucket.
 *
 *   LinkedHashSet can do all three but is insertion-ordered, not access-ordered.
 *   In practice, insertion-order IS access-order within a frequency bucket
 *   (because a key enters the bucket exactly once, on promotion). But in an
 *   interview the DLL version proves you understand what's happening under
 *   the hood and aren't just leaning on the standard library.
 *
 * ── INVARIANTS ─────────────────────────────────────────────────────────
 *
 *   1. After put() of a NEW key:          minFreq == 1
 *   2. Every key lives in EXACTLY ONE freqList bucket — the one matching its
 *      current frequency stored in node.freq.
 *   3. freqList.get(f).head is the LRU key at frequency f.
 *   4. freqList.get(f).tail is the MRU key at frequency f.
 *   5. freqList.get(f) never contains an empty DLL (if empty, entry is removed).
 *
 * ── TIME COMPLEXITY ────────────────────────────────────────────────────
 *
 *   keyAccessed(K) : O(1) — hash lookup + DLL unlink + DLL append
 *   evictKey()     : O(1) — pop head of minFreq list + hash removal
 *   removeKey(K)   : O(1) — hash lookup + DLL unlink + hash removal
 *
 * ── INTERVIEW CHEAT-SHEET ──────────────────────────────────────────────
 *
 *   Q: "How is LFU O(1)?"
 *   A: "HashMap&lt;key, node&gt; so I can find any node instantly. Each frequency
 *       has a DLL ordered by recency. A minFreq variable tells me which
 *       bucket to evict from. Three O(1) ops per access: unlink node from
 *       old freq list, append to new freq+1 list, update minFreq if needed."
 *
 *   Q: "Why doubly-linked, not singly?"
 *   A: "On get(), a key moves from freq=N to freq=N+1. I must unlink its
 *       specific node from the MIDDLE of list N. Singly-linked would need
 *       a scan from the head to find the predecessor. Doubly-linked is O(1)
 *       because each node knows its own prev pointer."
 *
 *   Q: "Tie-break between same frequency?"
 *   A: "The DLL within that bucket is access-ordered. HEAD was accessed the
 *       longest ago among all keys at this frequency. TAIL was accessed
 *       most recently. Eviction takes HEAD — true LRU within the bucket."
 *
 * ── DIFF FROM O1LFUEvictionPolicy ──────────────────────────────────────
 *
 *   O1LFUEvictionPolicy    : LinkedHashSet per bucket → insertion-order
 *                             (OK in practice, same result for standard LFU)
 *   TrueLRULFUEvictionPolicy: DLL per bucket → true access-order
 *                             (classic paper implementation, shows depth)
 *
 *   To swap between them in LFUCacheDemo, change ONE line:
 *     new O1LFUEvictionPolicy<>()       →  new TrueLRULFUEvictionPolicy<>()
 */
public class TrueLRULFUEvictionPolicy<K> implements EvictionPolicy<K> {

    // ── Node ───────────────────────────────────────────────────────────

    /**
     * A node in a frequency bucket's doubly-linked list.
     *
     * Stores the key and its current frequency so we know which freqList
     * bucket this node lives in. The prev/next pointers allow O(1) unlink.
     */
    private static class Node<K> {
        final K key;
        int freq;           // current frequency (1-based after first access)
        Node<K> prev;
        Node<K> next;

        Node(K key, int freq) {
            this.key = key;
            this.freq = freq;
        }
    }

    // ── DoublyLinkedList ───────────────────────────────────────────────

    /**
     * A doubly-linked list ordered by last-access time.
     *
     *   head = least-recently accessed node at this frequency
     *   tail = most-recently  accessed node at this frequency
     *
     * All methods are O(1). No sentinel nodes — we just null-check head/tail.
     */
    private static class DoublyLinkedList<K> {
        Node<K> head;
        Node<K> tail;
        int size;

        /**
         * Append node to the tail — it becomes the MRU key in this bucket.
         * Called when a key is promoted INTO this frequency.
         */
        void addToTail(Node<K> node) {
            if (tail == null) {
                head = tail = node;
                node.prev = node.next = null;
            } else {
                tail.next = node;
                node.prev = tail;
                node.next = null;
                tail = node;
            }
            size++;
        }

        /**
         * Unlink a specific node from anywhere in the list — head, middle, or tail.
         * Called when a key is promoted OUT of this frequency (freq → freq+1)
         * or during removeKey().
         */
        void remove(Node<K> node) {
            if (node.prev != null) {
                node.prev.next = node.next;
            } else {
                head = node.next;           // node was head
            }
            if (node.next != null) {
                node.next.prev = node.prev;
            } else {
                tail = node.prev;           // node was tail
            }
            node.prev = node.next = null;   // detach for GC
            size--;
        }

        /** Pop and return the head (LRU key at this frequency). Called during eviction. */
        Node<K> removeHead() {
            if (head == null) return null;
            Node<K> victim = head;
            remove(head);
            return victim;
        }

        boolean isEmpty() {
            return size == 0;
        }
    }

    // ── Core state ─────────────────────────────────────────────────────

    /** key → its DLL node (so we can unlink it in O(1) when frequency changes) */
    private final Map<K, Node<K>> keyNode = new HashMap<>();

    /** frequency → DLL of keys at that frequency, ordered by recency */
    private final Map<Integer, DoublyLinkedList<K>> freqList = new HashMap<>();

    /**
     * The smallest frequency that currently has at least one key.
     * This is the bucket we evict from when capacity is exceeded.
     */
    private int minFreq = 0;

    // ── Public API (EvictionPolicy contract) ────────────────────────────

    /**
     * Called on every get() or put().
     *
     * NEW KEY:
     *   Create Node(freq=1) → add to freq=1 list tail → set minFreq=1.
     *
     * EXISTING KEY:
     *   Unlink from freq=N list → increment freq to N+1 → add to freq=N+1 tail.
     *   If freq=N list is now empty AND N == minFreq → bump minFreq.
     */
    @Override
    public synchronized void keyAccessed(K key) {
        Node<K> node = keyNode.get(key);

        if (node != null) {
            // ── EXISTING KEY: promote from freq=N to freq=N+1 ───────────
            int oldFreq = node.freq;

            // Unlink from old frequency list
            DoublyLinkedList<K> oldList = freqList.get(oldFreq);
            oldList.remove(node);
            if (oldList.isEmpty()) {
                freqList.remove(oldFreq);
                if (minFreq == oldFreq) {
                    minFreq = oldFreq + 1;  // minFreq advances since old bucket is empty
                }
            }

            // Promote to freq+1 and append to new list (becomes MRU in that bucket)
            node.freq = oldFreq + 1;
        } else {
            // ── NEW KEY: insert at frequency 1 ──────────────────────────
            node = new Node<>(key, 1);
            keyNode.put(key, node);
            minFreq = 1;  // invariant: after any new insertion, minFreq == 1
        }

        freqList.computeIfAbsent(node.freq, k -> new DoublyLinkedList<>()).addToTail(node);
    }

    /**
     * Evicts the LFU key. Tie-break: within the minFreq bucket, evicts
     * the key that was accessed LEAST recently (head of the DLL).
     *
     * Returns null only if the cache is empty (freqList has no entries).
     */
    @Override
    public synchronized K evictKey() {
        DoublyLinkedList<K> minList = freqList.get(minFreq);
        if (minList == null || minList.isEmpty()) return null;

        Node<K> victim = minList.removeHead();
        if (minList.isEmpty()) {
            freqList.remove(minFreq);
            // minFreq will be reset when the next key is inserted (keyAccessed sets it to 1).
            // If another key exists at a higher frequency, we leave minFreq stale — but
            // that's safe because the next insert resets it, and the next get() only
            // bumps it upward (it can never jump down without an insert).
        }
        keyNode.remove(victim.key);
        return victim.key;
    }

    @Override
    public synchronized void removeKey(K key) {
        Node<K> node = keyNode.remove(key);
        if (node == null) return;

        DoublyLinkedList<K> list = freqList.get(node.freq);
        if (list != null) {
            list.remove(node);
            if (list.isEmpty()) {
                freqList.remove(node.freq);
            }
        }
    }
}
