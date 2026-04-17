# HashSet & LinkedHashSet Internal Implementation

## Table of Contents
1. [The Core Insight](#the-core-insight)
2. [HashSet = HashMap Facade](#hashset--hashmap-facade)
3. [Internal Implementation](#internal-implementation)
4. [Visual Diagrams](#visual-diagrams)
5. [Operation Breakdown](#operation-breakdown)
6. [LinkedHashSet](#linkedhashset)
7. [Memory Efficiency](#memory-efficiency)

---

## The Core Insight

**HashSet is NOT a separate data structure!**

It is a **DECORATOR/FACADE** over a `HashMap<K, Object>`.

```
┌─────────────────────────────────────────────────────────┐
│  HASHSET = HASHMAP FACADE                                │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  HashSet<E>  ──────►  HashMap<E, Object>                │
│                                                          │
│  The Set element  =  The Map KEY                        │
│  (what you care about)   (used for uniqueness)          │
│                                                          │
│  Dummy value      =  The Map VALUE                      │
│  (PRESENT object)      (ignored completely)             │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

## HashSet = HashMap Facade

### Actual Java Implementation

```java
public class HashSet<E> extends AbstractSet<E> implements Set<E> {
    
    // The underlying HashMap
    private transient HashMap<E, Object> map;
    
    // Shared dummy value used for ALL entries
    private static final Object PRESENT = new Object();
    
    // Constructor creates the backing HashMap
    public HashSet() {
        map = new HashMap<>();
    }
    
    // All operations delegate to the map
    
    public boolean add(E e) {
        return map.put(e, PRESENT) == null;
        // Returns true if key didn't exist (new addition)
        // Returns false if key existed (duplicate)
    }
    
    public boolean contains(Object o) {
        return map.containsKey(o);
    }
    
    public boolean remove(Object o) {
        return map.remove(o) == PRESENT;
    }
    
    public int size() {
        return map.size();
    }
    
    // iterator() returns map.keySet().iterator()
}
```

### Visual Representation

```
What you see as user:          What actually exists internally:
                              
HashSet<String> set =          HashMap<String, Object> map =
    new HashSet<>();              new HashMap<>();
                              
┌─────────────┐              ┌─────────────────────────────┐
│   HashSet   │              │         HashMap             │
│             │              │  ┌──────────┬──────────┐     │
│  "claude"   │◄────────────│  │ Key      │ Value    │     │
│  "sonnet"   │────────────►│  ├──────────┼──────────┤     │
│  "opus"     │             │  │ "claude" │ PRESENT  │     │
│             │             │  │ "sonnet" │ PRESENT  │     │
└─────────────┘             │  │ "opus"   │ PRESENT  │     │
                            │  └──────────┴──────────┘     │
                            └─────────────────────────────┘
                                          ↑
                                          │
                                    ┌──────────┐
                                    │ PRESENT  │
                                    │ (single  │
                                    │  dummy   │
                                    │  Object) │
                                    └──────────┘
                                    (one instance
                                     shared by all)
```

---

## Internal Implementation

### The PRESENT Object

```
┌─────────────────────────────────────────────────────────┐
│  WHY A DUMMY VALUE?                                       │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  HashMap contract requires K → V mapping               │
│                                                          │
│  HashMap doesn't have a "key-only" mode               │
│                                                          │
│  Solution: Use same dummy Object for ALL values         │
│                                                          │
│  ┌─────────────────────────────────────────────────┐   │
│  │  private static final Object PRESENT =            │   │
│  │      new Object();                                │   │
│  │                                                  │   │
│  │  One object created, referenced by all entries  │   │
│  │  Memory efficient: O(1) extra space, not O(n)   │   │
│  └─────────────────────────────────────────────────┘   │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Add Operation in Detail

```
Operation: set.add("claude")

Step 1: Delegate to map.put()
        map.put("claude", PRESENT)
        
Step 2: HashMap puts the entry
        ┌─────────────────────────┐
        │ table[index] →          │
        │   Entry {               │
        │     hash: hashCode(),   │
        │     key: "claude",      │
        │     value: PRESENT, ←───┼── Same object
        │     next: null          │    for all entries!
        │   }                     │
        └─────────────────────────┘

Step 3: Return result
        • If "claude" didn't exist: put() returns null
          → add() returns true (successfully added)
          
        • If "claude" existed: put() returns old value (PRESENT)
          → add() returns false (duplicate, not added)
```

### Contains Operation

```
Operation: set.contains("sonnet")

Direct delegation:
    set.contains("sonnet")
    └──► map.containsKey("sonnet")
         └──► HashMap finds key in table
              └──► Returns true/false

No need to check values - we only care about key existence!
```

### Remove Operation

```
Operation: set.remove("claude")

Step 1: map.remove("claude") is called

Step 2: If key exists:
        • Entry is removed from hash table
        • Returns PRESENT (the old value)
        • set.remove() returns true
        
        If key doesn't exist:
        • Returns null
        • set.remove() returns false

Step 3: Size decreases by 1
```

---

## Visual Diagrams

### Complete HashSet Structure

```
┌─────────────────────────────────────────────────────────────────────┐
│                        HASHSET STRUCTURE                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  HashSet<String>                                                     │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │  map: HashMap<String, Object>                                  │ │
│  │                                                                │ │
│  │  ┌──────────────────────────────────────────────────────────┐ │ │
│  │  │                    HASH TABLE                             │ │ │
│  │  │  ┌─────────┬─────────┬─────────┬─────────┬─────────┐      │ │ │
│  │  │  │ [0]     │ [1]     │ [2]     │ [3]     │ [4]     │      │ │ │
│  │  │  │  null   │ Entry   │  null   │ Entry   │ Entry   │      │ │ │
│  │  │  │         │   ↓     │         │   ↓     │         │      │ │ │
│  │  │  └─────────┴────┬────┴─────────┴────┬────┴─────────┘      │ │ │
│  │  │                │                   │                      │ │ │
│  │  │           ┌────┴────┐         ┌────┴────┐                  │ │ │
│  │  │           │ Entry   │         │ Entry   │                  │ │ │
│  │  │           │ key: A  │         │ key: C  │                  │ │ │
│  │  │           │ val: ■  │         │ val: ■  │                  │ │ │
│  │  │           │ next: ▼ │         │ next: ○ │                  │ │ │
│  │  │           │   ┌─────┴──┐      │         │                  │ │ │
│  │  │           │   │ Entry  │      │         │                  │ │ │
│  │  │           │   │ key: B │      │         │                  │ │ │
│  │  │           │   │ val: ■ │      │         │                  │ │ │
│  │  │           │   │ next:○ │      │         │                  │ │ │
│  │  │           │   └────────┘      │         │                  │ │ │
│  │  │           └───────────────────┘         │                  │ │ │
│  │  │                                          │                  │ │ │
│  │  └──────────────────────────────────────────────────────────┘ │ │
│  │                                                                │ │
│  │  PRESENT = new Object() ◄────────────────────────────────────────┘ │
│  │     ▲                                                            │
│  │     │                                                            │
│  │     └───── All entry.values reference this SAME object ──────────┘
│  └────────────────────────────────────────────────────────────────┘
│                                                                      │
│  ■ = reference to PRESENT (same object everywhere)                  │
│  ○ = null                                                            │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### HashSet vs LinkedHashSet

```
┌─────────────────────────────┐    ┌─────────────────────────────┐
│       HashSet               │    │      LinkedHashSet          │
│                             │    │                             │
│  Uses: HashMap              │    │  Uses: LinkedHashMap        │
│                             │    │                             │
│  Iteration order:           │    │  Iteration order:           │
│  UNPREDICTABLE              │    │  INSERTION ORDER            │
│                             │    │                             │
│  (hash bucket order)        │    │  (doubly linked list order) │
│                             │    │                             │
│  Example:                   │    │  Example:                   │
│  add A, add B, add C        │    │  add A, add B, add C        │
│                             │    │                             │
│  Iteration might give:      │    │  Iteration gives:           │
│  B, C, A (any order)        │    │  A, B, C (predictable)      │
│                             │    │                             │
└─────────────────────────────┘    └─────────────────────────────┘
```

---

## Operation Breakdown

### Complete Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    ADD OPERATION                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  User calls: set.add("element")                                 │
│                                                                  │
│         │                                                        │
│         ▼                                                        │
│  ┌─────────────────────────────┐                                 │
│  │ return map.put(e, PRESENT)  │                                 │
│  │        == null;             │                                 │
│  └─────────────────────────────┘                                 │
│         │                                                        │
│         ▼                                                        │
│  ┌─────────────────────────────┐                                 │
│  │  HashMap.put(key, value)   │                                 │
│  │                            │                                 │
│  │  1. hash(key)              │                                 │
│  │  2. find bucket            │                                 │
│  │  3. check if key exists    │                                 │
│  │     - if yes: replace val, │                                 │
│  │       return old val       │                                 │
│  │     - if no: add new entry,│                                 │
│  │       return null          │                                 │
│  └─────────────────────────────┘                                 │
│         │                                                        │
│         ▼                                                        │
│  ┌─────────────────────────────┐                                 │
│  │ Result interpretation:      │                                 │
│  │                            │                                 │
│  │ return null → true          │ (new element added)            │
│  │ return PRESENT → false      │ (duplicate, not added)         │
│  └─────────────────────────────┘                                 │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                  CONTAINS OPERATION                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  User calls: set.contains("element")                              │
│                                                                  │
│         │                                                        │
│         ▼                                                        │
│  ┌─────────────────────────────┐                                 │
│  │ return map.containsKey(e);   │                                 │
│  └─────────────────────────────┘                                 │
│         │                                                        │
│         ▼                                                        │
│  ┌─────────────────────────────┐                                 │
│  │  HashMap.containsKey(key)  │                                 │
│  │                            │                                 │
│  │  1. hash(key)              │                                 │
│  │  2. find bucket            │                                 │
│  │  3. traverse chain         │                                 │
│  │  4. key.equals() check     │                                 │
│  │  5. return true/false      │                                 │
│  └─────────────────────────────┘                                 │
│                                                                  │
│  Note: Value (PRESENT) is NEVER inspected!                       │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## LinkedHashSet

### Inheritance Chain

```
LinkedHashSet extends HashSet
        │
        └──► Uses LinkedHashMap internally

Construction chain:
new LinkedHashSet<>()
    │
    └──► HashSet(int initialCapacity, float loadFactor, boolean dummy)
            │
            └──► this.map = new LinkedHashMap<>(capacity, loadFactor)
```

### Visual Structure

```
┌─────────────────────────────────────────────────────────────────────┐
│                    LINKEDHASHSET STRUCTURE                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  LinkedHashSet<String>                                               │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │  map: LinkedHashMap<String, Object>                           │ │
│  │                                                                │ │
│  │  ┌──────────────────────────────────────────────────────────┐ │ │
│  │  │  HASH TABLE (for O(1) operations)                         │ │ │
│  │  │  ┌─────────┬─────────┬─────────┬─────────┬─────────┐      │ │ │
│  │  │  │ [0]     │ [1]     │ [2]     │ [3]     │ [4]     │      │ │ │
│  │  │  │  null   │ EntryA  │  null   │ EntryB  │ EntryC  │      │ │ │
│  │  │  └─────────┴─────────┴─────────┴─────────┴─────────┘      │ │ │
│  │  └──────────────────────────────────────────────────────────┘ │ │
│  │                                                                │ │
│  │  ┌──────────────────────────────────────────────────────────┐ │ │
│  │  │  DOUBLY LINKED LIST (for insertion order iteration)      │ │ │
│  │  │                                                            │ │ │
│  │  │   head                                                 tail│ │ │
│  │  │     ↓                                                    ↓ │ │ │
│  │  │   ┌──────────┐←───────────→┌──────────┐←───────────→┌───────┐│ │ │
│  │  │   │ EntryA   │   after     │ EntryB   │   after     │EntryC ││ │ │
│  │  │   │ key: A   │────────────→│ key: B   │────────────→│key: C ││ │ │
│  │  │   │ val: ■   │             │ val: ■   │             │val: ■ ││ │ │
│  │  │   │ before:○ │←────────────│ before: A│←────────────│bef: B ││ │ │
│  │  │   └──────────┘             └──────────┘             └───────┘│ │ │
│  │  │                                                            │ │ │
│  │  └──────────────────────────────────────────────────────────┘ │ │
│  │                                                                │ │
│  │  ■ = reference to PRESENT (shared dummy object)               │ │
│  │  ○ = null                                                      │ │
│  └────────────────────────────────────────────────────────────────┘
│                                                                      │
│  Iteration follows DLL: A → B → C (insertion order)                 │
│  NOT hash table bucket order!                                       │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Memory Efficiency

### Why Shared PRESENT Object?

```
┌─────────────────────────────────────────────────────────────────┐
│              MEMORY COMPARISON                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  OPTION 1: What Java does (EFFICIENT)                          │
│  ─────────────────────────────────────                          │
│                                                                  │
│  private static final Object PRESENT = new Object();            │
│                                                                  │
│  set.add("A") → map.put("A", PRESENT);                          │
│  set.add("B") → map.put("B", PRESENT);                          │
│  set.add("C") → map.put("C", PRESENT);                          │
│                                                                  │
│  Memory: 1 Object instance + 3 references                       │
│          = O(1) space overhead                                    │
│                                                                  │
│  ┌─────────┐                                                      │
│  │ PRESENT │◄────────────────────────────────────────┐            │
│  │  ████   │                                         │            │
│  └────┬────┘                                         │            │
│       │                                              │            │
│       │         ┌─────────┐    ┌─────────┐    ┌─────┴───┐        │
│       │         │ Entry A │    │ Entry B │    │ Entry C │        │
│       │         │ val: ───┼────┼───      │    │ val: ───┘        │
│       │         └─────────┘    └─────────┘    └─────────┘        │
│       │                                                          │
│       └─────────────────────────────────────────────────────────┘
│                                                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  OPTION 2: New object each time (WASTEFUL - NOT used)            │
│  ────────────────────────────────────────────────────            │
│                                                                  │
│  set.add("A") → map.put("A", new Object());                      │
│  set.add("B") → map.put("B", new Object());                      │
│  set.add("C") → map.put("C", new Object());                      │
│                                                                  │
│  Memory: 3 separate Object instances                              │
│          = O(n) space overhead                                    │
│                                                                  │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐                      │
│  │ Object1 │    │ Object2 │    │ Object3 │  ← 3 allocations!   │
│  │  ████   │    │  ████   │    │  ████   │                      │
│  └────┬────┘    └────┬────┘    └────┬────┘                      │
│       │              │              │                              │
│       ↓              ↓              ↓                            │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐                      │
│  │ Entry A │    │ Entry B │    │ Entry C │                      │
│  │ val: ───┼───→│ val: ───┼───→│ val: ───┘                      │
│  └─────────┘    └─────────┘    └─────────┘                      │
│                                                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Since values are NEVER used in HashSet,                        │
│  using one shared instance saves n-1 Object allocations!          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Summary

| Aspect | HashSet | LinkedHashSet |
|--------|---------|---------------|
| **Underlying Map** | `HashMap` | `LinkedHashMap` |
| **add()** | O(1) | O(1) |
| **remove()** | O(1) | O(1) |
| **contains()** | O(1) | O(1) |
| **Iteration Order** | Unpredictable | Insertion order |
| **Space** | O(n) | O(n) |

### Key Takeaways

1. **HashSet is a facade**: It wraps a HashMap, using keys as set elements and a shared dummy value

2. **PRESENT object**: Single shared `Object` instance used as value for all entries - memory efficient

3. **Operations delegate**: `add()` → `put()`, `contains()` → `containsKey()`, `remove()` → `remove()`

4. **LinkedHashSet = HashSet + order**: Uses LinkedHashMap internally for predictable iteration order

5. **No separate data structure**: HashSet doesn't duplicate functionality - it reuses HashMap's battle-tested implementation

> **Design Pattern**: This is an excellent example of **Composition over Inheritance** and the **Facade Pattern** - wrapping a complex implementation (HashMap) with a simpler interface (Set).
