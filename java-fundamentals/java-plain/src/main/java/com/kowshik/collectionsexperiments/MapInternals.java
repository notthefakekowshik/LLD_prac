package com.kowshik.collectionsexperiments;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ============================================================================
 * HASHMAP & LINKEDHASHMAP INTERNAL IMPLEMENTATION EXPLANATION
 * ============================================================================
 *
 * =============================================================================
 * PART 1: HASHMAP INTERNALS
 * =============================================================================
 *
 * HASHMAP STRUCTURE (Java 8+):
 * ---------------------------
 * HashMap is an array of Node<K,V>[] called "table"
 * Each index in this array is a "bucket"
 * Each bucket can be:
 *   - null (empty bucket)
 *   - A single Node (linked list head)
 *   - A TreeNode (red-black tree root, when list grows > 8)
 *
 * NODE STRUCTURE:
 * ---------------
 * class Node<K,V> {
 *     final int hash;      // cached hash of key
 *     final K key;         // the key
 *     V value;             // the value
 *     Node<K,V> next;      // next node in linked list (collision chain)
 * }
 *
 * PUT OPERATION FLOW:
 * -------------------
 * put("claude", "king")
 *
 * Step 1: Calculate hash
 *   hash("claude") = some integer (hashCode with spread logic)
 *
 * Step 2: Find bucket index
 *   index = (table.length - 1) & hash  // equivalent to hash % table.length
 *
 * Step 3: Place in bucket
 *   - If bucket is null: create new Node, place it there
 *   - If bucket has node(s): check for key equality, replace if same key
 *     otherwise append to end of linked list
 *
 * COLLISION HANDLING:
 * ------------------
 * When two keys hash to same bucket index:
 *   - Keys are chained in a linked list at that bucket
 *   - Example: put("A", 1) and put("B", 2) both hash to index 5
 *
 *   Bucket[5] → Node("A",1) → Node("B",2) → null
 *
 *   - Search in this bucket: O(n) where n = chain length
 *   - Java 8+: When chain > 8, converts to Red-Black Tree for O(log n)
 *
 * GET OPERATION FLOW:
 * -------------------
 * get("claude")
 *
 * Step 1: Calculate hash of "claude" (same hash function)
 * Step 2: Find bucket index
 * Step 3: Traverse the chain at that bucket, compare key.equals()
 * Step 4: Return value if found, null otherwise
 *
 * TIME COMPLEXITY:
 * ----------------
 * - Best/Average case: O(1) - uniform hash distribution
 * - Worst case (all keys collide): O(n) or O(log n) with tree
 *
 *
 * =============================================================================
 * PART 2: LINKEDHASHMAP INTERNALS - THE KEY INSIGHT!
 * =============================================================================
 *
 * YOUR QUESTION ANSWERED:
 * -----------------------
 * "If I do put("claude", "king") in LHM, does it create a DLL node
 *  with a linked list inside containing 'claude'?"
 *
 * ANSWER: NO! This is the common misconception.
 *
 * LINKEDHASHMAP HAS TWO SEPARATE STRUCTURES:
 * ------------------------------------------
 *
 * STRUCTURE 1: THE HASH TABLE (for O(1) access)
 * ---------------------------------------------
 *   - Same as HashMap: array of Entry<K,V>[] buckets
 *   - Used for put(), get(), containsKey() - all O(1) operations
 *   - Entry extends HashMap.Node, adds:
 *     - Entry<K,V> before;  // reference for doubly linked list
 *     - Entry<K,V> after;   // reference for doubly linked list
 *
 * STRUCTURE 2: THE DOUBLY LINKED LIST (for insertion order / LRU)
 * ---------------------------------------------------------------
 *   - Head (oldest/least recently used)
 *   - Tail (newest/most recently used)
 *   - Links entries in insertion OR access order
 *
 * VISUAL REPRESENTATION OF LHM:
 * -----------------------------
 *
 * After: put("claude", "king");
 *         put("sonnet", "queen");
 *         put("opus", "ace");
 *
 * HASH TABLE (for O(1) lookup):
 * -----------------------------
 *   Index 0: null
 *   Index 1: Entry("claude","king",hash=123) → null
 *   Index 2: null
 *   Index 3: Entry("sonnet","queen",hash=456) → null
 *   Index 4: Entry("opus","ace",hash=789) → null
 *   ...
 *
 * DOUBLY LINKED LIST (for iteration order):
 * -----------------------------------------
 *   head                                              tail
 *     ↓                                                ↓
 *   +----------------+    +----------------+    +----------------+
 *   | "claude"="king"|───→|"sonnet"="queen"│───→│  "opus"="ace"  │
 *   │ hash=123       │←────│ hash=456       │←───│ hash=789       │
 *   │ before=null    │    │ before=claude  │    │ before=sonnet  │
 *   │ after=sonnet   │    │ after=opus     │    │ after=null     │
 *   +----------------+    +----------------+    +----------------+
 *
 * KEY INSIGHT:
 * ------------
 * Each Entry object lives in BOTH structures simultaneously!
 * - Its 'next' pointer is for the hash bucket chain
 * - Its 'before'/'after' pointers are for the DLL
 *
 * PUT("claude", "king") - DETAILED STEP BY STEP:
 * ----------------------------------------------
 *
 * Step 1: Hash "claude" → hash = 123
 *
 * Step 2: Find bucket index = 123 & (table.length-1) = 1
 *
 * Step 3: Create Entry object:
 *   {
 *     hash: 123,
 *     key: "claude",
 *     value: "king",
 *     next: null,        // hash bucket chain (no collision yet)
 *     before: null,      // DLL - this is the first entry
 *     after: null        // DLL - nothing after yet
 *   }
 *
 * Step 4: Place in hash table:
 *   table[1] = entry
 *
 * Step 5: Link in DLL:
 *   head = entry
 *   tail = entry
 *
 * PUT("sonnet", "queen") - SECOND INSERTION:
 * ------------------------------------------
 *
 * Step 1: Hash "sonnet" → hash = 456
 * Step 2: Find bucket index = 3 (different bucket!)
 *
 * Step 3: Create Entry:
 *   {
 *     hash: 456,
 *     key: "sonnet",
 *     value: "queen",
 *     next: null,
 *     before: null,   // will be updated
 *     after: null     // will be updated
 *   }
 *
 * Step 4: Place in hash table:
 *   table[3] = entry
 *
 * Step 5: Link in DLL (at TAIL):
 *   tail.after = newEntry;   // old tail points to new entry
 *   newEntry.before = tail;  // new entry points back to old tail
 *   tail = newEntry;         // update tail reference
 *
 *   Now:
 *   head = "claude" entry
 *   tail = "sonnet" entry
 *
 *   DLL: claude ←→ sonnet
 *
 * PUT("opus", "ace") - THIRD INSERTION:
 * -------------------------------------
 *
 * Step 1-4: Same process, placed in hash table at its bucket
 *
 * Step 5: Link in DLL at tail:
 *   "sonnet".after = "opus" entry;
 *   "opus".before = "sonnet" entry;
 *   tail = "opus" entry;
 *
 *   DLL: claude ←→ sonnet ←→ opus
 *
 * WHAT IF SAME BUCKET? (COLLISION IN LHM):
 * -----------------------------------------
 * put("claude", "king");      // hash=123, goes to bucket 1
 * put("CLOUDE", "emperor");   // let's say hash=123 too, same bucket 1
 *
 * HASH TABLE at bucket 1:
 *   Entry("claude","king") → Entry("CLOUDE","emperor") → null
 *                        ↑
 *                        next pointer (collision chain)
 *
 * DOUBLY LINKED LIST:
 *   claude ←→ CLOUDE (insertion order, NOT bucket order!)
 *
 * KEY POINT: The DLL is GLOBAL across ALL buckets, not per-bucket!
 *
 *
 * ACCESS ORDER MODE (for LRU Cache):
 * ------------------------------------
 * LinkedHashMap<Integer, String> lru = new LinkedHashMap<>(16, 0.75f, true);
 * // true = access order, false = insertion order (default)
 *
 * After: put(1, "A"); put(2, "B"); put(3, "C");
 * DLL: 1 ←→ 2 ←→ 3
 *
 * get(2);  // Access element 2
 * // DLL is reordered: 1 ←→ 3 ←→ 2
 * // 2 moved to tail (most recently used)
 *
 * PUT with EXISTING KEY (Update):
 * -------------------------------
 * put("claude", "emperor");  // key already exists
 *
 * Step 1: Find existing entry via hash table (O(1))
 * Step 2: Update value in place
 * Step 3: If accessOrder=true, move entry to tail of DLL
 *
 *
 * TIME COMPLEXITY SUMMARY:
 * ------------------------
 *                     HashMap    LinkedHashMap
 * put()               O(1)       O(1)
 * get()               O(1)       O(1)  ← YES, O(1)!
 * remove()            O(1)       O(1)
 * containsKey()       O(1)       O(1)
 * iteration           O(n)       O(n)  ← but LHM has predictable order
 *
 * The DLL does NOT affect get() performance!
 * get() uses the hash table, not the DLL.
 */
public class MapInternals {

    public static void main(String[] args) {
        demonstrateHashMap();
        demonstrateLinkedHashMapInsertionOrder();
        demonstrateLinkedHashMapAccessOrder();
        demonstrateCollisionScenario();
    }

    static void demonstrateHashMap() {
        System.out.println("=== HASHMAP DEMONSTRATION ===\n");

        Map<String, String> hashMap = new HashMap<>();

        System.out.println("1. Initial empty HashMap:");
        System.out.println("   table = Node[16] (default initial capacity)");
        System.out.println("   All 16 buckets are null\n");

        System.out.println("2. put(\"claude\", \"king\"):");
        hashMap.put("claude", "king");
        System.out.println("   - hash(\"claude\") → some integer value");
        System.out.println("   - index = hash & 15 (since table.length=16)");
        System.out.println("   - Creates: new Node(hash, \"claude\", \"king\", next=null)");
        System.out.println("   - Places at table[index]\n");

        System.out.println("3. put(\"sonnet\", \"queen\"):");
        hashMap.put("sonnet", "queen");
        System.out.println("   - Different key, likely different hash");
        System.out.println("   - Different index in table");
        System.out.println("   - Another Node created at different bucket\n");

        System.out.println("4. put(\"opus\", \"ace\"):");
        hashMap.put("opus", "ace");
        System.out.println("   - Third Node at (probably) third bucket\n");

        System.out.println("Current HashMap: " + hashMap);
        System.out.println("Note: Iteration order is UNPREDICTABLE (based on hash distribution)\n");
    }

    static void demonstrateLinkedHashMapInsertionOrder() {
        System.out.println("\n=== LINKEDHASHMAP - INSERTION ORDER ===\n");

        // false = insertion order (default)
        LinkedHashMap<String, String> lhm = new LinkedHashMap<>(16, 0.75f, false);

        System.out.println("Creating: new LinkedHashMap<>(16, 0.75f, false)");
        System.out.println("false = insertion order mode\n");

        System.out.println("1. put(\"claude\", \"king\"):");
        lhm.put("claude", "king");
        System.out.println("   Hash Table: Entry at bucket[index1]");
        System.out.println("   DLL: head=claude, tail=claude");
        System.out.println("   (before=null, after=null)\n");

        System.out.println("2. put(\"sonnet\", \"queen\"):");
        lhm.put("sonnet", "queen");
        System.out.println("   Hash Table: Entry at bucket[index2] (different bucket!)");
        System.out.println("   DLL: claude ←→ sonnet");
        System.out.println("   head=claude, tail=sonnet");
        System.out.println("   claude.after=sonnet, sonnet.before=claude\n");

        System.out.println("3. put(\"opus\", \"ace\"):");
        lhm.put("opus", "ace");
        System.out.println("   Hash Table: Entry at bucket[index3]");
        System.out.println("   DLL: claude ←→ sonnet ←→ opus");
        System.out.println("   head=claude, tail=opus\n");

        System.out.println("Iteration order (predictable):");
        for (Map.Entry<String, String> entry : lhm.entrySet()) {
            System.out.println("   " + entry.getKey() + " = " + entry.getValue());
        }
        System.out.println();

        System.out.println("4. get(\"sonnet\") - DOES NOT change order in insertion mode:");
        lhm.get("sonnet");
        System.out.println("   DLL remains: claude ←→ sonnet ←→ opus\n");
    }

    static void demonstrateLinkedHashMapAccessOrder() {
        System.out.println("\n=== LINKEDHASHMAP - ACCESS ORDER (LRU Pattern) ===\n");

        // true = access order
        LinkedHashMap<String, String> lru = new LinkedHashMap<>(16, 0.75f, true);

        System.out.println("Creating: new LinkedHashMap<>(16, 0.75f, true)");
        System.out.println("true = access order mode (LRU behavior)\n");

        lru.put("claude", "king");
        lru.put("sonnet", "queen");
        lru.put("opus", "ace");

        System.out.println("After 3 puts, DLL: claude ←→ sonnet ←→ opus");
        System.out.println("head (LRU)=claude, tail (MRU)=opus\n");

        System.out.println("get(\"sonnet\"):  // Access middle element");
        lru.get("sonnet");
        System.out.println("   - Found via hash table in O(1)");
        System.out.println("   - Moved to tail of DLL (now most recently used)");
        System.out.println("   DLL: claude ←→ opus ←→ sonnet\n");

        System.out.println("get(\"claude\"):  // Access head element");
        lru.get("claude");
        System.out.println("   - Moved to tail");
        System.out.println("   DLL: opus ←→ sonnet ←→ claude\n");

        System.out.println("Current order (MRU at end):");
        for (Map.Entry<String, String> entry : lru.entrySet()) {
            System.out.println("   " + entry.getKey() + " = " + entry.getValue());
        }
        System.out.println();

        System.out.println("LRU Cache Implementation Pattern:");
        System.out.println("   - Override removeEldestEntry()");
        System.out.println("   - When size > capacity, eldest (head) is auto-removed");
    }

    static void demonstrateCollisionScenario() {
        System.out.println("\n=== COLLISION SCENARIO IN LINKEDHASHMAP ===\n");

        LinkedHashMap<String, String> lhm = new LinkedHashMap<>();

        System.out.println("Scenario: Two keys hash to same bucket (collision)\n");

        lhm.put("claude", "king");
        System.out.println("put(\"claude\", \"king\"):");
        System.out.println("   Hash table bucket[5]: Entry(\"claude\") → null");
        System.out.println("   DLL: claude (head & tail)\n");

        // These strings are designed to potentially collide
        // In real scenario, depends on actual hashCode values
        System.out.println("put(\"Aa\", \"value1\") then put(\"BB\", \"value2\"):");
        System.out.println("   (Note: \"Aa\" and \"BB\" can have same hashCode in Java!)");

        lhm.put("Aa", "value1");
        lhm.put("BB", "value2");

        System.out.println("   If they collide at bucket[10]:");
        System.out.println("   Hash table bucket[10]: Entry(\"Aa\") → Entry(\"BB\") → null");
        System.out.println("                            ↑\n");
        System.out.println("                            next pointer (collision chain)");
        System.out.println("   BUT DLL (global): claude ←→ Aa ←→ BB");
        System.out.println("   (DLL tracks insertion order across ALL buckets)\n");

        System.out.println("get(\"BB\"):");
        System.out.println("   1. hash(\"BB\") → find bucket[10]");
        System.out.println("   2. Traverse chain: check Aa.equals(BB)? No");
        System.out.println("                      check BB.equals(BB)? Yes!");
        System.out.println("   3. Return value - still O(1) if chain is short\n");

        System.out.println("Current LHM content: " + lhm);
        System.out.println("Size: " + lhm.size());
    }
}
