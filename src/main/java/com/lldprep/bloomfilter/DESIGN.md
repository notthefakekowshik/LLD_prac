# Bloom Filter - Low-Level Design Document

**Problem**: Design an in-memory Bloom Filter for space-efficient probabilistic membership testing.

**Date**: 2026-03-26  
**Complexity**: O(k) for add/contains operations (k = number of hash functions)

---

## Step 1: DEFINE — Requirements & Constraints

### Functional Requirements (Actor-Verb-Noun)

- **FR1**: User can add an element to the Bloom filter
- **FR2**: User can check if an element *might* exist (with possible false positives, but zero false negatives)
- **FR3**: System calculates optimal bit array size and number of hash functions based on expected capacity and desired false positive rate
- **FR4**: System supports generic types (not limited to strings)
- **FR5**: User can configure false positive rate at construction time

### Non-Functional Requirements

- **NFR1**: **Time Complexity** — O(k) for both add() and contains() operations, where k = number of hash functions
- **NFR2**: **Space Efficiency** — Significantly smaller memory footprint than storing actual elements
- **NFR3**: **Thread Safety** — Safe for concurrent reads and writes
- **NFR4**: **Configurability** — False positive rate is configurable (default 1%)

### Constraints

- **C1**: **No Deletion** — Standard Bloom filter does not support element removal
- **C2**: **In-Memory Only** — No persistence or serialization required
- **C3**: **Fixed Capacity** — Expected number of elements must be known at construction time
- **C4**: **Immutable Size** — Bit array size is fixed after creation (no dynamic resizing)

### Out of Scope

- Counting Bloom filters (which support deletion via counters)
- Distributed Bloom filters across multiple nodes
- Bloom filter merging or intersection operations
- Persistence to disk

---

## Step 2: IDENTIFY — Entities & Relationships

### Noun-Verb Analysis

**Nouns** (Candidate Entities):
- Bloom Filter → `BloomFilter<T>` (main class)
- Hash Function → `HashFunction<T>` (interface)
- Bit Array → `BitSet` (Java built-in, composition)
- Builder → `BloomFilterBuilder<T>` (nested class)
- Hash Strategy → Concrete implementations (MurmurHash, FNV, Simple)

**Verbs** (Candidate Methods):
- "add element" → `add(T element)`
- "check membership" → `mightContain(T element)`
- "hash element" → `hash(T element, int seed)`
- "calculate optimal size" → `calculateOptimalBitArraySize(int expectedElements, double fpp)`
- "calculate hash count" → `calculateOptimalHashCount(int bitArraySize, int expectedElements)`
- "build filter" → `build()`

### Relationships

| From | To | Type | Meaning |
|------|-----|------|---------|
| `BloomFilter<T>` | `HashFunction<T>` | **Dependency** (DIP) | BloomFilter depends on HashFunction abstraction |
| `BloomFilter<T>` | `BitSet` | **Composition** | BloomFilter owns BitSet, cannot exist without it |
| `BloomFilter<T>` | `BloomFilterBuilder<T>` | **Association** | Builder creates BloomFilter instances |
| `MurmurHashFunction<T>` | `HashFunction<T>` | **Realization** | Implements the hash strategy |
| `FNVHashFunction<T>` | `HashFunction<T>` | **Realization** | Implements the hash strategy |
| `SimpleHashFunction<T>` | `HashFunction<T>` | **Realization** | Implements the hash strategy |

---

## Step 3: Design Patterns Applied

### 1. **Strategy Pattern** ⭐ (Primary)

**Problem**: Multiple hash function algorithms exist, and they should be interchangeable without modifying BloomFilter.

**Solution**: Define `HashFunction<T>` interface. BloomFilter depends on the abstraction, not concrete implementations.

**Benefit**: 
- Open/Closed Principle — adding CityHash or XXHash requires zero changes to BloomFilter
- Different use cases can choose speed vs. quality trade-offs

**Code Structure**:
```
HashFunction<T> (interface)
    ├── MurmurHashFunction<T>
    ├── FNVHashFunction<T>
    └── SimpleHashFunction<T>
```

---

### 2. **Builder Pattern** ⭐ (Primary)

**Problem**: BloomFilter construction requires multiple interdependent parameters:
- Expected elements (required)
- False positive rate (optional, default 0.01)
- Hash function (optional, default MurmurHash)
- Calculated: bit array size, number of hash functions

**Solution**: Nested `BloomFilter.Builder<T>` class with fluent API.

**Benefit**:
- Readable, self-documenting construction
- Automatic calculation of optimal parameters
- Validation before object creation
- Immutable BloomFilter after construction

**Usage Example**:
```java
BloomFilter<String> filter = new BloomFilter.Builder<String>()
    .expectedElements(10000)
    .falsePositiveRate(0.01)
    .hashFunction(new MurmurHashFunction<>())
    .build();
```

---

### 3. **Facade Pattern** (Implicit)

**Problem**: Bloom filter internals are complex (bit manipulation, multiple hash functions, mathematical calculations).

**Solution**: `BloomFilter<T>` provides a simple public API (`add()`, `mightContain()`) that hides:
- BitSet management
- Hash function orchestration
- Bit position calculations

**Benefit**: Callers don't need to understand the underlying complexity.

---

### 4. **Template Method Pattern** (Considered, but NOT used)

**Decision**: Initially considered for `add()` and `contains()` since they share the same skeleton (generate k hashes → map to positions → set/check bits). However, the logic is simple enough that extracting a template method would be **premature abstraction**. We'll keep the methods explicit for clarity.

---

## Step 4: Class Diagram (Mermaid.js)

```mermaid
classDiagram
    class BloomFilter~T~ {
        -BitSet bitArray
        -HashFunction~T~ hashFunction
        -int bitArraySize
        -int numHashFunctions
        -int expectedElements
        -double falsePositiveRate
        
        +add(T element) void
        +mightContain(T element) boolean
        +getExpectedElements() int
        +getFalsePositiveRate() double
        -getBitPosition(T element, int hashIndex) int
    }
    
    class Builder~T~ {
        -int expectedElements
        -double falsePositiveRate
        -HashFunction~T~ hashFunction
        
        +expectedElements(int n) Builder~T~
        +falsePositiveRate(double fpp) Builder~T~
        +hashFunction(HashFunction~T~ hf) Builder~T~
        +build() BloomFilter~T~
        -calculateOptimalBitArraySize() int
        -calculateOptimalHashCount() int
    }
    
    class HashFunction~T~ {
        <<interface>>
        +hash(T element, int seed) int
    }
    
    class MurmurHashFunction~T~ {
        +hash(T element, int seed) int
    }
    
    class FNVHashFunction~T~ {
        +hash(T element, int seed) int
    }
    
    class SimpleHashFunction~T~ {
        +hash(T element, int seed) int
    }
    
    class BitSet {
        <<Java Built-in>>
        +set(int index) void
        +get(int index) boolean
    }
    
    BloomFilter~T~ *-- BitSet : owns
    BloomFilter~T~ --> HashFunction~T~ : depends on (DIP)
    Builder~T~ ..> BloomFilter~T~ : creates
    BloomFilter~T~ +-- Builder~T~ : nested class
    
    HashFunction~T~ <|.. MurmurHashFunction~T~ : implements
    HashFunction~T~ <|.. FNVHashFunction~T~ : implements
    HashFunction~T~ <|.. SimpleHashFunction~T~ : implements
```

---

## Step 5: SOLID Principles Compliance

### ✅ Single Responsibility Principle (SRP)
- `BloomFilter<T>` — manages membership testing logic
- `HashFunction<T>` — generates hash values
- `Builder<T>` — constructs and validates BloomFilter configuration
- `BitSet` — manages bit array operations

### ✅ Open/Closed Principle (OCP)
- Adding a new hash function (e.g., CityHash) = new class implementing `HashFunction<T>`
- Zero modifications to existing `BloomFilter` code

### ✅ Liskov Substitution Principle (LSP)
- Any `HashFunction<T>` implementation can replace another
- All implementations honor the contract: deterministic hash for same input+seed

### ✅ Interface Segregation Principle (ISP)
- `HashFunction<T>` has exactly one method: `hash()`
- No fat interfaces forcing unnecessary implementations

### ✅ Dependency Inversion Principle (DIP)
- `BloomFilter` depends on `HashFunction<T>` interface, not concrete classes
- Hash function is injected via Builder (constructor injection)

---

## Step 6: Mathematical Foundations

### Optimal Bit Array Size
```
m = -(n * ln(p)) / (ln(2)^2)

where:
  m = bit array size
  n = expected number of elements
  p = desired false positive rate
```

### Optimal Number of Hash Functions
```
k = (m / n) * ln(2)

where:
  k = number of hash functions
  m = bit array size
  n = expected number of elements
```

### Actual False Positive Rate
```
p_actual = (1 - e^(-kn/m))^k

where:
  k = number of hash functions
  n = number of elements inserted
  m = bit array size
```

---

## Step 7: Package Structure

```
com.lldprep.bloomfilter/
    ├── DESIGN.md                    (this file)
    ├── README.md                    (usage examples, design choices)
    ├── model/
    │   └── BloomFilterConfig.java   (optional: encapsulate config)
    ├── hash/
    │   ├── HashFunction.java        (interface)
    │   ├── MurmurHashFunction.java
    │   ├── FNVHashFunction.java
    │   └── SimpleHashFunction.java
    ├── BloomFilter.java             (main class with nested Builder)
    ├── exception/
    │   └── BloomFilterException.java
    └── BloomFilterDemo.java         (Main class)
```

---

## Step 8: Curveball Scenarios (To be handled in Evolve phase)

| Curveball | How to Handle |
|-----------|---------------|
| **"Add support for deletion"** | Implement Counting Bloom Filter — replace BitSet with int array, decrement on delete |
| **"Support serialization"** | Implement Serializable, add toBytes()/fromBytes() methods |
| **"Add a reset() method"** | Add `clear()` method that resets all bits to 0 |
| **"Support merging two Bloom filters"** | Add `merge(BloomFilter other)` — requires same size/hash config, OR bits |
| **"Add monitoring/metrics"** | Add `getApproximateCount()`, `getSaturation()`, `estimateFalsePositiveRate()` |
| **"Support dynamic resizing"** | Implement Scalable Bloom Filter — chain multiple fixed-size filters |

---

## Interview Communication Notes

**What I would say during the interview**:

1. **After requirements**: "I'll use the Strategy pattern for hash functions since we may want to swap algorithms, and Builder pattern since construction involves complex parameter calculations."

2. **During design**: "I'm making `HashFunction` an interface because different use cases may prioritize speed (SimpleHash) vs. quality (MurmurHash). This follows the Open/Closed Principle."

3. **Trade-offs**: "I chose composition over inheritance for BitSet because Bloom filter 'has-a' bit array, not 'is-a' bit array. Also, BitSet is a final class in Java, so inheritance isn't possible."

4. **Proactive extensibility**: "After the base implementation, I can extend this to support Counting Bloom Filters for deletion, or Scalable Bloom Filters for dynamic resizing."

---

**Next Steps**: Implement the design following this document.
