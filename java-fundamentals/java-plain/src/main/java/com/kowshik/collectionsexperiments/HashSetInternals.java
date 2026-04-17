package com.kowshik.collectionsexperiments;

import java.util.HashSet;
import java.util.LinkedHashSet;

/**
 * ============================================================================
 * HASHSET INTERNAL IMPLEMENTATION EXPLANATION
 * ============================================================================
 *
 * THE CORE INSIGHT:
 * -----------------
 * HashSet is NOT a separate data structure!
 * It is a DECORATOR/FACADE over a HashMap<K, Object>
 *
 * HashSet uses HashMap internally where:
 *   - The Set's element becomes the HashMap's KEY
 *   - The HashMap's VALUE is always the same dummy object: new Object()
 *
 * =============================================================================
 * HASHSET INTERNAL STRUCTURE
 * =============================================================================
 *
 * DECLARATION IN JAVA:
 * --------------------
 * public class HashSet<E> extends AbstractSet<E> implements Set<E> {
 *     private transient HashMap<E,Object> map;
 *     private static final Object PRESENT = new Object();  // Dummy value
 *
 *     public HashSet() {
 *         map = new HashMap<>();
 *     }
 *
 *     public boolean add(E e) {
 *         return map.put(e, PRESENT) == null;
 *     }
 *
 *     public boolean contains(Object o) {
 *         return map.containsKey(o);
 *     }
 *
 *     public boolean remove(Object o) {
 *         return map.remove(o) == PRESENT;
 *     }
 *
 *     // ... other methods delegate to map
 * }
 *
 *
 * VISUAL REPRESENTATION:
 * ----------------------
 *
 * HashSet<String> set = new HashSet<>();
 * set.add("claude");
 * set.add("sonnet");
 * set.add("opus");
 *
 * What you see:          What actually exists (HashMap inside):
 * -----------            ----------------------------------------
 * HashSet                HashMap<String, Object>
 * ┌─────────┐           ┌─────────────────────────────┐
 * │ "claude"│           │ Key          │ Value       │
 * │ "sonnet"│           ├──────────────┼─────────────┤
 * │ "opus"  │           │ "claude"     │ PRESENT     │ ← dummy Object
 * └─────────┘           │ "sonnet"     │ PRESENT     │ ← same dummy Object
 *                       │ "opus"       │ PRESENT     │ ← same dummy Object
 *                       └─────────────────────────────┘
 *
 * The HashSet just ignores the values - it only cares about keys!
 *
 *
 * ADD OPERATION:
 * --------------
 * add("claude") calls: map.put("claude", PRESENT)
 *
 * Returns:
 *   - true if put() returns null (key didn't exist - new addition)
 *   - false if put() returns old value (key already existed - duplicate)
 *
 * CONTAINS OPERATION:
 * -------------------
 * contains("claude") calls: map.containsKey("claude")
 *
 * Returns true if the key exists in the underlying HashMap
 *
 * REMOVE OPERATION:
 * -----------------
 * remove("claude") calls: map.remove("claude")
 *
 * Returns true if a mapping was removed (element existed)
 *
 *
 * =============================================================================
 * LINKEDHASHSET INTERNALS
 * =============================================================================
 *
 * LinkedHashSet extends HashSet but uses LinkedHashMap internally:
 *
 * public class LinkedHashSet<E> extends HashSet<E> implements Set<E> {
 *     public LinkedHashSet() {
 *         super(16, .75f, true);  // calls HashSet constructor with dummy
 *     }
 * }
 *
 * In HashSet:
 * HashSet(int initialCapacity, float loadFactor, boolean dummy) {
 *     map = new LinkedHashMap<>(initialCapacity, loadFactor);
 * }
 *
 * So LinkedHashSet = HashMap facade over LinkedHashMap
 *
 * This gives:
 *   - O(1) add, remove, contains (hash table)
 *   - Predictable iteration order (insertion order DLL)
 *
 *
 * ITERATOR BEHAVIOR:
 * ------------------
 * HashSet.iterator():      Iterates in unpredictable order (hash bucket order)
 * LinkedHashSet.iterator(): Iterates in insertion order (DLL order)
 *
 *
 * TIME COMPLEXITY SUMMARY:
 * ------------------------
 * Operation         HashSet    LinkedHashSet
 * add()             O(1)       O(1)
 * remove()          O(1)       O(1)
 * contains()        O(1)       O(1)
 * iteration         O(n)       O(n) - but predictable order
 *
 * Space: O(n) for both
 *
 *
 * WHY USE PRESENT DUMMY OBJECT?
 * -----------------------------
 * 1. HashMap requires a value - can't have null for all (would confuse with missing keys)
 * 2. Using same static Object saves memory (one instance shared)
 * 3. Using new Object() per entry would waste memory unnecessarily
 *
 * The PRESENT is just a placeholder to satisfy HashMap contract.
 */
public class HashSetInternals {

    // Dummy object reference to demonstrate
    private static final Object PRESENT = new Object();

    public static void main(String[] args) {
        demonstrateHashSetIsHashMapFacade();
        demonstrateLinkedHashSet();
        demonstrateSetOperations();
        demonstrateMemoryEfficiency();
    }

    static void demonstrateHashSetIsHashMapFacade() {
        System.out.println("=== HASHSET = HASHMAP FACADE ===\n");

        System.out.println("Core Concept:");
        System.out.println("  HashSet<E> is essentially: HashMap<E, Object>");
        System.out.println("  Where the Set element is the Map KEY");
        System.out.println("  And the Map VALUE is always a dummy Object\n");

        HashSet<String> set = new HashSet<>();

        System.out.println("Creating: new HashSet<>()\n");

        System.out.println("Behind the scenes:");
        System.out.println("  this.map = new HashMap<>();\n");

        System.out.println("add(\"claude\"):");
        set.add("claude");
        System.out.println("  → map.put(\"claude\", PRESENT)");
        System.out.println("  PRESENT is a static final Object instance\n");

        System.out.println("add(\"sonnet\"):");
        set.add("sonnet");
        System.out.println("  → map.put(\"sonnet\", PRESENT)\n");

        System.out.println("add(\"opus\"):");
        set.add("opus");
        System.out.println("  → map.put(\"opus\", PRESENT)\n");

        System.out.println("Internal state (conceptual):");
        System.out.println("  HashMap table:");
        System.out.println("    Bucket 3:  Entry(\"claude\", PRESENT) → null");
        System.out.println("    Bucket 7:  Entry(\"sonnet\", PRESENT) → null");
        System.out.println("    Bucket 12: Entry(\"opus\", PRESENT) → null");
        System.out.println("  (Actual bucket indices depend on hash codes)\n");

        System.out.println("Set contents (iteration order unpredictable):");
        for (String s : set) {
            System.out.println("  " + s);
        }
        System.out.println();
    }

    static void demonstrateLinkedHashSet() {
        System.out.println("\n=== LINKEDHASHSET = LINKEDHASHMAP FACADE ===\n");

        LinkedHashSet<String> lhs = new LinkedHashSet<>();

        System.out.println("Creating: new LinkedHashSet<>()\n");

        System.out.println("Behind the scenes:");
        System.out.println("  LinkedHashSet constructor calls:");
        System.out.println("    super(16, 0.75f, true);");
        System.out.println("  Which creates:");
        System.out.println("    this.map = new LinkedHashMap<>();\n");

        System.out.println("Adding elements:");
        lhs.add("claude");
        lhs.add("sonnet");
        lhs.add("opus");

        System.out.println("  add(\"claude\") → LinkedHashMap.put(\"claude\", PRESENT)");
        System.out.println("    Hash table: Entry at bucket[X]");
        System.out.println("    DLL: head=claude, tail=claude\n");

        System.out.println("  add(\"sonnet\") → LinkedHashMap.put(\"sonnet\", PRESENT)");
        System.out.println("    Hash table: Entry at bucket[Y]");
        System.out.println("    DLL: claude ←→ sonnet (linked in insertion order)\n");

        System.out.println("  add(\"opus\") → LinkedHashMap.put(\"opus\", PRESENT)");
        System.out.println("    Hash table: Entry at bucket[Z]");
        System.out.println("    DLL: claude ←→ sonnet ←→ opus\n");

        System.out.println("Set contents (predictable insertion order):");
        for (String s : lhs) {
            System.out.println("  " + s);
        }
        System.out.println();

        System.out.println("Key difference from HashSet:");
        System.out.println("  - HashSet iterates in hash bucket order (unpredictable)");
        System.out.println("  - LinkedHashSet iterates in DLL order (insertion order)\n");
    }

    static void demonstrateSetOperations() {
        System.out.println("\n=== SET OPERATIONS DELEGATE TO MAP ===\n");

        HashSet<String> set = new HashSet<>();

        // Build the set
        set.add("claude");
        set.add("sonnet");

        System.out.println("Set: " + set + "\n");

        System.out.println("1. ADD OPERATION:");
        System.out.println("   add(\"opus\")");
        boolean added1 = set.add("opus");
        System.out.println("   → map.put(\"opus\", PRESENT) returns null");
        System.out.println("   → add returns: " + added1 + " (element was new)");

        System.out.println("\n   add(\"claude\")  // duplicate!");
        boolean added2 = set.add("claude");
        System.out.println("   → map.put(\"claude\", PRESENT) returns PRESENT (old value)");
        System.out.println("   → add returns: " + added2 + " (element already existed)\n");

        System.out.println("2. CONTAINS OPERATION:");
        System.out.println("   contains(\"sonnet\")");
        boolean contains = set.contains("sonnet");
        System.out.println("   → map.containsKey(\"sonnet\")");
        System.out.println("   → returns: " + contains + "\n");

        System.out.println("   contains(\"gemini\")");
        boolean notContains = set.contains("gemini");
        System.out.println("   → map.containsKey(\"gemini\")");
        System.out.println("   → returns: " + notContains + "\n");

        System.out.println("3. REMOVE OPERATION:");
        System.out.println("   remove(\"claude\")");
        boolean removed = set.remove("claude");
        System.out.println("   → map.remove(\"claude\") returns PRESENT");
        System.out.println("   → remove returns: " + removed + " (element was present)\n");

        System.out.println("   remove(\"gpt\")  // not present");
        boolean notRemoved = set.remove("gpt");
        System.out.println("   → map.remove(\"gpt\") returns null");
        System.out.println("   → remove returns: " + notRemoved + "\n");

        System.out.println("Final set: " + set);
    }

    static void demonstrateMemoryEfficiency() {
        System.out.println("\n=== MEMORY EFFICIENCY ===\n");

        System.out.println("Why use static PRESENT object instead of new Object() each time?\n");

        System.out.println("Option 1 (What Java does - efficient):");
        System.out.println("  private static final Object PRESENT = new Object();");
        System.out.println("  add(\"A\") → map.put(\"A\", PRESENT);");
        System.out.println("  add(\"B\") → map.put(\"B\", PRESENT);");
        System.out.println("  add(\"C\") → map.put(\"C\", PRESENT);");
        System.out.println("  Memory: 1 dummy object shared by all entries\n");

        System.out.println("Option 2 (Wasteful - not used):");
        System.out.println("  add(\"A\") → map.put(\"A\", new Object());");
        System.out.println("  add(\"B\") → map.put(\"B\", new Object());");
        System.out.println("  add(\"C\") → map.put(\"C\", new Object());");
        System.out.println("  Memory: N dummy objects for N entries\n");

        System.out.println("Since HashSet doesn't use the values at all,");
        System.out.println("using one shared instance saves memory.\n");

        System.out.println("PRESENT object identity check:");
        System.out.println("  PRESENT == PRESENT: " + (PRESENT == PRESENT));
        System.out.println("  (Same reference everywhere)\n");

        System.out.println("SUMMARY:");
        System.out.println("  HashSet leverages HashMap's key uniqueness");
        System.out.println("  and fast lookup while ignoring values.");
        System.out.println("  This is a classic example of composition over inheritance");
        System.out.println("  and effective use of existing data structures.");
    }
}
