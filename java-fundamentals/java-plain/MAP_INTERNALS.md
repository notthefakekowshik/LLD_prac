# HashMap & LinkedHashMap Internal Implementation

## Table of Contents
1. [HashMap Internals](#hashmap-internals)
2. [LinkedHashMap Internals](#linkedhashmap-internals)
3. [The Key Insight: Two Parallel Structures](#the-key-insight)
4. [Step-by-Step Operations](#step-by-step-operations)
5. [Common Misconception Clarified](#common-misconception-clarified)

---

## HashMap Internals

### Core Structure

HashMap is an **array of Node<K,V>[]** called `table`. Each index is a "bucket".

```
HashMap Structure:
┌─────────────────────────────────────────────────────────────┐
│  table: Node<K,V>[]                                         │
│  ┌─────────┬─────────┬─────────┬─────────┬─────────┐         │
│  │ [0]     │ [1]     │ [2]     │ [3]     │ [4]     │ ...    │
│  │  null   │ NodeA ──┼→ NodeB │  null   │ NodeC   │        │
│  └─────────┴────┬────┴─────────┴─────────┴─────────┘         │
│                 │                                            │
│                 ↓ next                                        │
│              ┌────────┐                                      │
│              │ NodeB  │ (collision chain)                  │
│              │ key=B  │                                      │
│              │ next=null│                                     │
│              └────────┘                                      │
└─────────────────────────────────────────────────────────────┘
```

### Node Structure

```java
class Node<K,V> {
    final int hash;      // cached hash of key
    final K key;         // the key
    V value;             // the value
    Node<K,V> next;      // next node in linked list (collision chain)
}
```

### Put Operation Flow

**`put("claude", "king")`**

```
Step 1: Calculate Hash
        hash("claude") → 123456

Step 2: Find Bucket Index
        index = (table.length - 1) & hash
        index = 15 & 123456 → 0 (for example)

Step 3: Create Node
        ┌─────────────────────┐
        │ Node                │
        ├─────────────────────┤
        │ hash: 123456        │
        │ key: "claude"       │
        │ value: "king"       │
        │ next: null          │
        └─────────────────────┘

Step 4: Place in Table
        table[0] → Node("claude", "king")
```

### Collision Handling

When two keys hash to the same bucket:

```
Before collision:
table[5] → Node("A", 1) → null

After put("B", 2) with same hash:
table[5] → Node("A", 1) ──next──→ Node("B", 2) → null
                ↓                         ↓
             key="A"                    key="B"

Search at bucket[5]:
1. Check "A".equals(target)?
2. No → follow next
3. Check "B".equals(target)?
4. Yes → return value
```

> **Java 8+ optimization**: When chain length > 8, converts to Red-Black Tree for O(log n) lookup instead of O(n).

---

## LinkedHashMap Internals

### The Core Concept

**LinkedHashMap = HashMap + Doubly Linked List**

It maintains **TWO SEPARATE STRUCTURES** in parallel:

| Structure | Purpose | Used By |
|-----------|---------|---------|
| **Hash Table** | O(1) key lookup | `get()`, `put()`, `remove()` |
| **Doubly Linked List** | Maintain insertion/access order | Iteration, LRU cache |

```
┌─────────────────────────────────────────────────────────────────┐
│                    LINKEDHASHMAP STRUCTURE                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. HASH TABLE (for O(1) operations)                            │
│  ┌─────────┬─────────┬─────────┬─────────┬─────────┐             │
│  │ [0]     │ [1]     │ [2]     │ [3]     │ [4]     │             │
│  │  null   │ EntryA  │  null   │ EntryB  │ EntryC  │             │
│  └─────────┴─────────┴─────────┴─────────┴─────────┘             │
│                                                                  │
│  2. DOUBLY LINKED LIST (for order)                              │
│                                                                  │
│  head                                                         tail│
│    ↓                                                           ↓ │
│  ┌──────────┐      ┌──────────┐      ┌──────────┐              │
│  │ EntryA   │←────→│ EntryB   │←────→│ EntryC   │              │
│  │ "claude" │after │ "sonnet" │after │ "opus"   │              │
│  │ before=null│    │ before=A │      │ before=B │              │
│  │ after=B  │      │ after=C  │      │ after=null│             │
│  └──────────┘      └──────────┘      └──────────┘              │
│                                                                  │
│  Each Entry lives in BOTH structures simultaneously!            │
│  • 'next' pointer → for hash bucket chain                       │
│  • 'before/after' → for DLL (global across all buckets)       │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Entry Structure

```java
class Entry<K,V> extends HashMap.Node<K,V> {
    // From HashMap.Node:
    final int hash;
    final K key;
    V value;
    Node<K,V> next;     // for hash collision chain
    
    // Added by LinkedHashMap:
    Entry<K,V> before;  // previous in DLL
    Entry<K,V> after;   // next in DLL
}
```

---

## The Key Insight

### Your Question Answered

> "If I do `put("claude", "king")` in LHM, does it create a DLL node with a linked list inside containing 'claude'?"

**NO!** This is the common misconception.

**Correct Understanding:**
- **ONE Entry object** is created per key-value pair
- This Entry goes into **BOTH** structures simultaneously
- The DLL is **global** across all hash buckets, not per-bucket

```
WRONG mental model (misconception):
┌─────────────────────────────────────┐
│ DLL Node                            │
│ ┌─────────────────────────────────┐ │
│ │ "claude" key                    │ │
│ │ ┌────────┬────────┐             │ │
│ │ │ claude │→ null   │ (inner LL)  │ │
│ │ └────────┴────────┘             │ │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘

CORRECT mental model:
┌─────────────────────────────────────────────────────────┐
│                    One Entry Object                      │
├─────────────────────────────────────────────────────────┤
│  Hash Table View:            DLL View:                   │
│  ┌──────────────┐           ┌──────────────┐            │
│  │ bucket[5]    │           │ before       │←───┐       │
│  │ ↓            │           │ "claude"     │    │       │
│  │ ┌──────────┐ │           │ key          │    │       │
│  │ │ Entry    │ │           │ value        │    │       │
│  │ │ next─────┼─┼──→ null   │ after        │───┐│       │
│  │ └──────────┘ │           └──────────────┘   ││       │
│  └──────────────┘                             ││       │
│                                                ↓↓       │
│                                           ┌──────────┐  │
│                                           │ next     │  │
│                                           │ Entry    │  │
│                                           └──────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## Step-by-Step Operations

### Operation 1: `put("claude", "king")`

```
Initial state: Empty LHM

table = Entry[16] (all null)
head = null, tail = null

Step 1: hash("claude") → 123
Step 2: index = 123 & 15 = 11

Step 3: Create Entry:
┌──────────────────────────┐
│ Entry("claude", "king")  │
├──────────────────────────┤
│ hash: 123                │
│ key: "claude"            │
│ value: "king"            │
│ next: null  ←─┐          │
│ before: null  │          │
│ after: null   │          │
└───────────────┼──────────┘
                │
  For hash      │  For DLL
  collisions    │  (insertion order)
                ↓

Step 4: Place in hash table
table[11] → Entry("claude", "king")

Step 5: Link in DLL
head = Entry("claude")
tail = Entry("claude")

Result:
table[11]: Entry("claude") → null
DLL: claude (head=tail, isolated)
```

### Operation 2: `put("sonnet", "queen")`

```
State after first put:
• table[11] = Entry("claude")
• DLL: claude (head=tail)

Step 1: hash("sonnet") → 456 (different hash!)
Step 2: index = 456 & 15 = 8 (different bucket!)

Step 3: Create Entry:
┌───────────────────────────┐
│ Entry("sonnet", "queen")   │
├───────────────────────────┤
│ hash: 456                 │
│ key: "sonnet"             │
│ value: "queen"            │
│ next: null                │
│ before: null  ←── will link to "claude"
│ after: null   ←── will link from "claude"
└───────────────────────────┘

Step 4: Place in hash table
table[8] → Entry("sonnet", "queen")

Step 5: Link at TAIL of DLL
tail.after = newEntry    // "claude".after = "sonnet"
newEntry.before = tail   // "sonnet".before = "claude"
tail = newEntry          // tail now points to "sonnet"

Result:
HASH TABLE:               DOUBLY LINKED LIST:
table[11]: claude ──→     head                    tail
                          ↓                       ↓
table[8]:  sonnet ──→    ┌────────┐←───────────→┌────────┐
                         │claude  │   after     │sonnet  │
                         │before=null│←────────│before=cld│
                         │after=son │           │after=null│
                         └────────┘             └────────┘

Note: "claude" and "sonnet" are in DIFFERENT hash buckets
      but linked together in the GLOBAL DLL!
```

### Operation 3: `put("opus", "ace")`

```
Step 1: hash("opus") → 789
Step 2: index = 789 & 15 = 4 (yet another bucket!)

Step 3: Create Entry("opus", "ace")

Step 4: table[4] → Entry("opus")

Step 5: Link at tail:
"sonnet".after = "opus"
"opus".before = "sonnet"
tail = "opus"

Final State:
┌─────────────────────────────────────────────────────────────┐
│ HASH TABLE                                                  │
├─────────────────────────────────────────────────────────────┤
│ Index 4:  Entry("opus")  ──→ null                           │
│ Index 8:  Entry("sonnet")──→ null                           │
│ Index 11: Entry("claude")──→ null                           │
│ Others:   null                                               │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ DOUBLY LINKED LIST (insertion order)                        │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│   head                                                    tail│
│     ↓                                                      ↓ │
│   ┌─────────┐←─────────────→┌─────────┐←─────────────→┌────────┐│
│   │ claude  │    after      │ sonnet  │    after      │ opus   ││
│   │ before  │←──────────────│ before  │←──────────────│ before ││
│   │ =null   │               │ =claude │               │ =sonnet││
│   │ after   │──────────────→│ after   │──────────────→│ after  ││
│   │ =sonnet │               │ =opus   │               │ =null  ││
│   └─────────┘               └─────────┘               └────────┘│
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### Collision Scenario in LinkedHashMap

```
put("claude", "king");   // hash=123 → bucket 11
put("CLOUDE", "emperor"); // hash=123 → bucket 11 (SAME!)

HASH TABLE bucket[11]:
┌────────────────────────────────────────┐
│ Entry("claude") ──next──→ Entry("CLOUDE") ──→ null │
│   ↓                          ↓         │
│ hash:123                   hash:123    │
│ key:"claude"               key:"CLOUDE" │
└────────────────────────────────────────┘

DOUBLY LINKED LIST (global, NOT per-bucket):
┌────────────────────────────────────────┐
│ head                                   │
│   ↓                                    │
│ claude ←────────→ CLOUDE              │
│          after   before                │
└────────────────────────────────────────┘

KEY POINT: DLL links entries in INSERTION ORDER,
not in bucket collision chain order!
```

### Get Operation (Your Misconception Clarified)

```
get("sonnet")

WRONG PATH (what you might have thought):
   Start at head of DLL
   → traverse DLL: claude → sonnet
   → found! Time: O(n)

ACTUAL PATH (what really happens):
   Step 1: hash("sonnet") → 456
   Step 2: index = 456 & 15 = 8
   Step 3: Go directly to table[8]
   Step 4: Check Entry("sonnet")
   Step 5: Return "queen"
   
   Time: O(1) ✓
   
   No DLL traversal needed!
   The DLL is completely bypassed for get() operations.
```

---

## Common Misconception Clarified

### The Misconception

> "LinkedHashMap `get()` is not O(1) because of the doubly linked list"

**This is WRONG!**

### The Truth

```
┌─────────────────────────────────────────────────────────────┐
│  LinkedHashMap get() uses ONLY the hash table!              │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  get("key"):                                                  │
│    │                                                          │
│    ├─→ hash("key") ──→ O(1)                                   │
│    │                                                          │
│    ├─→ index = hash & (length-1) ──→ O(1)                     │
│    │                                                          │
│    ├─→ entry = table[index] ──→ O(1)                          │
│    │                                                          │
│    ├─→ traverse collision chain (usually 1-2 nodes) ──→ O(1)  │
│    │                                                          │
│    └─→ return entry.value                                     │
│                                                               │
│  ╔═════════════════════════════════════════════════════════╗  │
│  ║  The DLL is NEVER touched during get()!                 ║  │
│  ║  It's only used for:                                   ║  │
│  ║    • Iteration (entrySet, keySet, values)               ║  │
│  ║    • LRU cache reordering (if accessOrder=true)        ║  │
│  ╚═════════════════════════════════════════════════════════╝  │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### Time Complexity Summary

| Operation | HashMap | LinkedHashMap | Notes |
|-----------|---------|---------------|-------|
| `put()` | O(1) | O(1) | LHM extra: link at tail |
| `get()` | O(1) | **O(1)** | DLL not used for lookup! |
| `remove()` | O(1) | O(1) | LHM extra: unlink from DLL |
| `containsKey()` | O(1) | O(1) | |
| Iteration | O(n) | O(n) | LHM: predictable order |

### Access Order Mode (LRU Cache)

```java
LinkedHashMap<String, String> lru = 
    new LinkedHashMap<>(16, 0.75f, true); // true = access order
```

```
Initial: put A, put B, put C
DLL: A ←→ B ←→ C
head (LRU)     tail (MRU)

get(B):  // Access B
  1. Find B via hash table: O(1)
  2. Move B to tail of DLL
  
Result:
DLL: A ←→ C ←→ B
      ↑          ↑
    LRU         MRU

put(D):  // Add new entry
  1. Add to hash table: O(1)
  2. Add to tail of DLL: O(1)
  3. If size > capacity, remove head (LRU)
  
Result:
DLL: C ←→ B ←→ D  (A was evicted - least recently used)
```

---

## Key Takeaways

1. **LinkedHashMap has TWO structures**: Hash table (for O(1) operations) + Doubly linked list (for order)

2. **`get()` is O(1)** in LinkedHashMap - it uses the hash table, never traverses the DLL

3. **DLL is GLOBAL**: Links all entries across ALL hash buckets in insertion/access order

4. **Each Entry lives in both structures**: 
   - `next` pointer → hash collision chain (per-bucket)
   - `before/after` pointers → global DLL (across all buckets)

5. **HashSet = HashMap facade**: Keys are set elements, values are all a shared dummy `Object`
