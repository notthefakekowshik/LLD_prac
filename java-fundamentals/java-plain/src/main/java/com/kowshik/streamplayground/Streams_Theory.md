# Java Streams — Complete Theory

Streams operate on a **source** (collection, array, I/O), apply a **pipeline** of intermediate operations (lazy), and produce a **result** via a terminal operation (eager). The pipeline is evaluated only when a terminal op is called.

---

## 1. Stream Creation — 8 Ways

```java
// From Collection (most common)
Stream<String> s1 = List.of("a", "b").stream();

// From Array
Stream<Integer> s2 = Arrays.stream(new int[]{1, 2, 3}).boxed();

// From values
Stream<String> s3 = Stream.of("x", "y");

// From iterate (Java 9 seed + predicate)
Stream<Integer> s4 = Stream.iterate(1, n -> n < 100, n -> n * 2);  // 1, 2, 4, 8, 16...

// From generate (infinite)
Stream<Double> s5 = Stream.generate(Math::random);

// From Builder
Stream<String> s6 = Stream.<String>builder().add("a").add("b").build();

// From range (IntStream)
IntStream s7 = IntStream.range(1, 5);          // 1,2,3,4  (exclusive)
IntStream s8 = IntStream.rangeClosed(1, 5);    // 1,2,3,4,5 (inclusive)

// From I/O (Files.lines)
Stream<String> s9 = Files.lines(Path.of("data.txt"));
```

---

## 2. Intermediate Operations (Lazy — return Stream<T>)

| Operation | What it does | Key detail |
|-----------|-------------|------------|
| `filter(Predicate)` | Keep elements matching predicate | |
| `map(Function)` | Transform each element T → R | |
| `flatMap(Function)` | Transform each element T → Stream\<R\>, then flatten | 1-to-many transform |
| `distinct()` | Remove duplicates via `equals()` | Stateful — needs to see all elements |
| `sorted()` | Natural order sort | Stateful — needs entire stream buffered first |
| `sorted(Comparator)` | Custom order sort | Same as above |
| `peek(Consumer)` | Inspect each element (debugging) | Side-effect only; don't mutate! |
| `limit(n)` | Stop after n elements | Short-circuits |
| `skip(n)` | Skip first n elements | |
| `takeWhile(Predicate)` | Take while true, then stop (Java 9) | Short-circuits on first false |
| `dropWhile(Predicate)` | Drop while true, then pass rest (Java 9) | |

**Lazy evaluation means operations don't execute until a terminal op is called:**

```java
List<Integer> result = Stream.of(1, 2, 3, 4, 5)
    .filter(n -> { System.out.println("filter: " + n); return n % 2 == 0; })
    .map(n -> { System.out.println("map: " + n); return n * n; })
    .limit(1)                    // short-circuits after first!
    .toList();
// Prints: filter: 1, filter: 2, map: 2
// Never processes 3, 4, 5 because limit(1) satisfied.
```

---

## 3. Terminal Operations (Eager — produce a non-Stream result)

### 3.1 Collection

```java
// Basic collection
List<T>  list  = stream.toList();                    // Immutable (Java 16)
List<T>  list2 = stream.collect(Collectors.toList()); // Mutable (pre-Java 16)
Set<T>   set   = stream.collect(Collectors.toSet());
Map<K,V> map   = stream.collect(Collectors.toMap(keyFn, valFn)); // Throws on duplicate key!
Map<K,V> map2  = stream.collect(Collectors.toMap(keyFn, valFn, (a,b) -> b)); // Merge on dup

// Specialized
String   joined = stream.collect(Collectors.joining(", "));
String   prefixed = stream.collect(Collectors.joining(", ", "[", "]"));  // "[a, b, c]"
```

### 3.2 Grouping & Partitioning

```java
// groupBy: arbitrary key → map
Map<String, List<Person>> byCity = people.stream()
    .collect(Collectors.groupingBy(Person::city));

// groupBy with downstream collector
Map<String, Long> countByCity = people.stream()
    .collect(Collectors.groupingBy(Person::city, Collectors.counting()));

Map<String, Double> avgAgeByCity = people.stream()
    .collect(Collectors.groupingBy(Person::city, Collectors.averagingInt(Person::age)));

// Multi-level grouping
Map<String, Map<Gender, List<Person>>> byCityThenGender = people.stream()
    .collect(Collectors.groupingBy(Person::city, Collectors.groupingBy(Person::gender)));

// partitioningBy: boolean key (special case of groupBy)
Map<Boolean, List<Integer>> evens = numbers.stream()
    .collect(Collectors.partitioningBy(n -> n % 2 == 0));
// {true=[2,4,6], false=[1,3,5]}

// teeing: run two collectors, merge results (Java 12)
record Stats(long count, double avg) {}
Stats stats = people.stream()
    .collect(Collectors.teeing(
        Collectors.counting(),
        Collectors.averagingInt(Person::age),
        Stats::new
    ));
```

### 3.3 Reduction

```java
// reduce with identity (no Optional)
int sum = numbers.stream().reduce(0, Integer::sum);
// reduce without identity (returns Optional — stream might be empty)
Optional<Integer> max = numbers.stream().reduce(Integer::max);
// reduce with identity + accumulator + combiner (parallel-ready)
int parallelSum = numbers.parallelStream().reduce(0, Integer::sum, Integer::sum);

// Specialized: count, min, max, sum, average
long   count = stream.count();
Optional<T> min = stream.min(comparator);
Optional<T> max = stream.max(comparator);
OptionalDouble avg = intStream.average();
int sum = intStream.sum();          // IntStream only
```

### 3.4 Search / Match (Short-Circuiting)

```java
boolean anyPositive = stream.anyMatch(n -> n > 0);   // True if any element matches
boolean allPositive = stream.allMatch(n -> n > 0);   // True if ALL match (vacuously true on empty)
boolean noneZero    = stream.noneMatch(n -> n == 0);  // True if NONE match (vacuously true on empty)

Optional<T> first = stream.findFirst();  // First element (respects encounter order)
Optional<T> any   = stream.findAny();    // Any element (faster for parallel, order not guaranteed)
```

---

## 4. Collectors Deep Dive

### Predefined Collectors

| Collector | Input | Output | Example |
|-----------|-------|--------|---------|
| `toList()` | T | List\<T\> | `stream.collect(toList())` |
| `toSet()` | T | Set\<T\> | `stream.collect(toSet())` |
| `toMap()` | T | Map\<K,V\> | See above |
| `joining()` | CharSequence | String | `collect(joining(","))` |
| `counting()` | T | Long | `collect(groupingBy(k, counting()))` |
| `summarizingInt()` | T (int) | IntSummaryStatistics | count, sum, min, avg, max in one pass |
| `averagingInt()` | T (int) | Double | Average |
| `summingInt()` | T (int) | Integer | Sum |
| `maxBy(comp)` | T | Optional\<T\> | Max element |
| `minBy(comp)` | T | Optional\<T\> | Min element |
| `reducing()` | T | T | General reduction |
| `mapping(Fn, downstream)` | T | R | Map then accumulate |
| `filtering(Pred, downstream)` | T | T (Java 9) | Filter then accumulate |
| `flatMapping(Fn, down)` | T | R (Java 9) | FlatMap then accumulate |
| `collectingAndThen(...)` | T | R | Transform after collection |
| `teeing(c1, c2, merger)` | T | R (Java 12) | Two collectors merged |

### Custom Collector (rare but shows mastery)

```java
Collector<Integer, int[], double[]> standardDeviationCollector =
    Collector.of(
        () -> new int[]{0, 0},                             // supplier: [sum, count]
        (acc, val) -> { acc[0] += val; acc[1]++; },        // accumulator
        (acc1, acc2) -> { acc1[0] += acc2[0]; acc1[1] += acc2[1]; return acc1; }, // combiner
        acc -> {                                            // finisher
            double mean = (double) acc[0] / acc[1];
            // Need a second pass for variance...
            return new double[]{mean, 0};                   // simplified
        }
    );
```

---

## 5. Primitive Streams

Why they exist: `Stream<Integer>` boxes every int, killing cache locality and adding garbage. Primitive streams avoid boxing.

```java
// IntStream — methods not on Stream<T>
IntStream.range(1, 100)                     // 1..99
         .rangeClosed(1, 100)               // 1..100
         .sum()                             // int (not OptionalInt — empty = 0)
         .average()                         // OptionalDouble
         .summaryStatistics()               // IntSummaryStatistics{count, sum, min, avg, max}
         .mapToObj(Integer::toString)       // IntStream → Stream<String>
         .boxed()                           // IntStream → Stream<Integer>

// LongStream / DoubleStream — same API, different types
LongStream.range(1, 1_000_000).sum();
DoubleStream.of(1.0, 2.0).average();

// Conversion
Stream<Integer> s = intStream.boxed();              // to boxed
IntStream is = stream.mapToInt(Integer::intValue);  // to primitive
IntStream is2 = stream.flatMapToInt(...);           // 1-to-many to primitive
```

**Key gotcha:** `IntStream.sum()` returns `int` (0 if empty). `Stream<Integer>.mapToInt(Integer::intValue).sum()` is faster than `Stream<Integer>.reduce(0, Integer::sum)` because there's no boxing.

---

## 6. Parallel Streams — When & When NOT

### How they work

`stream.parallel()` splits the source into substreams, processes each on the common `ForkJoinPool.commonPool()` (number of cores - 1 threads), then merges results. The split-merge requires the source to be **splittable** and the pipeline to be **stateless**.

### When to use

```
✅ Large data sets (N > 10,000)
✅ CPU-bound operations
✅ Stateless, independent per-element operations
✅ Associative, non-interfering accumulator in reduce/collect
✅ ArrayList / Array / IntStream.range (cheaply splittable)
```

### When NOT to use

```
❌ Small data sets (splitting cost > parallelism gain)
❌ Stateful operations (distinct, sorted on small streams, limit)
❌ Shared mutable state in lambdas
❌ I/O-bound operations (fork-join pool threads block → starvation)
❌ LinkedList (O(n) to split — sequential is faster)
❌ Operations with ordering constraints (findFirst vs findAny)
❌ Already running inside a ForkJoinPool task
```

### Common Pitfall: Shared Mutable State

```java
// WRONG — non-thread-safe accumulation
List<Integer> results = new ArrayList<>();
intStream.parallel()
    .filter(n -> n % 2 == 0)
    .forEach(results::add);  // ConcurrentModificationException or lost elements

// RIGHT — use collector
List<Integer> results = intStream.parallel()
    .filter(n -> n % 2 == 0)
    .boxed()
    .collect(Collectors.toList());
```

### findFirst vs findAny

```java
// findFirst respects encounter order — forces ordering overhead in parallel
stream.parallel().findFirst();  // Slower than sequential if source is splittable

// findAny doesn't care about order — faster in parallel
stream.parallel().findAny();    // Returns any element from any substream
```

---

## 7. Infinite Streams & Short-Circuiting

```java
// iterate: Java 8 (infinite)
Stream.iterate(0, n -> n + 2)
    .limit(10)
    .forEach(System.out::print);  // 0 2 4 6 8 10 12 14 16 18

// iterate: Java 9 (finite with predicate)
Stream.iterate(0, n -> n < 20, n -> n + 2)
    .forEach(System.out::print);  // 0 2 4 6 8 10 12 14 16 18 (stops at 20)

// generate (infinite, no input dependency)
Stream.generate(() -> ThreadLocalRandom.current().nextInt(100))
    .limit(5)
    .forEach(System.out::println);

// takeWhile / dropWhile (Java 9)
List<Integer> sorted = List.of(1, 2, 3, 5, 8, 13, 21);
sorted.stream().takeWhile(n -> n < 10).toList();   // [1, 2, 3, 5, 8] — stops at 13
sorted.stream().dropWhile(n -> n < 10).toList();   // [13, 21] — skips until >= 10
```

---

## 8. reduce() — The Master Operation

`collect`, `sum`, `min`, `max`, `count` are all special cases of `reduce`. Understanding `reduce` gives you the mental model for everything.

```java
// reduce(identity, accumulator) — always returns T, not Optional
T result = stream.reduce(identity, (a, b) -> combined);
// identity: starting value (empty stream returns this)
// accumulator: how to combine two values

// reduce(accumulator) — returns Optional<T> (empty stream → Optional.empty())
Optional<T> result = stream.reduce((a, b) -> combined);

// reduce(identity, accumulator, combiner) — for parallel streams
U result = stream.reduce(identity, (U, T) -> U, (U, U) -> U);
// accumulator: how to fold a T into the partial result U
// combiner: how to merge two partial results U + U (used only in parallel)
```

**Why the combiner exists:** In parallel, each substream produces a partial result. The combiner merges partials. Without it, parallel reduce is incorrect.

```java
// Parallel sum: identity=0, accumulator=Integer::sum, combiner=Integer::sum
int sum = numbers.parallelStream().reduce(0, Integer::sum, Integer::sum);

// Parallel string concat (correct — associative, identity is "")
String concat = strings.parallelStream().reduce("", String::concat, String::concat);

// Compute length of all strings in parallel
int totalLen = strings.parallelStream().reduce(
    0,                             // identity
    (len, str) -> len + str.length(),  // accumulator: fold String into int
    Integer::sum                   // combiner: merge two partial ints
);
```

---

## 9. Performance & Memory

### Rule of Thumb

| Scenario | Use |
|----------|-----|
| Small data (N < 1,000) | Sequential stream or for-loop |
| Large data, CPU-bound | Parallel stream (test first!) |
| Large data, I/O-bound | Virtual threads or CompletableFuture, NOT parallel stream |
| Primitive-heavy computation | IntStream/LongStream/DoubleStream (no boxing) |
| LinkedList source | Don't parallelize (expensive split) |

### Boxing Cost Example

```java
// BAD — boxes every int
Stream<Integer> s = ints.stream();
s.map(n -> n * 2)
 .filter(n -> n > 10)
 .reduce(0, Integer::sum);

// GOOD — zero boxing
ints.stream()
    .mapToInt(Integer::intValue)
    .map(n -> n * 2)
    .filter(n -> n > 10)
    .sum();
```

---

## 10. Spliterator — How Streams Split For Parallelism

`Spliterator` is the bridge between a data source and a stream's split-merge engine. Every collection has a spliterator that controls how it's partitioned.

```java
// ArrayList — ideal splitting (O(1) by index range)
Spliterator<Integer> split = arrayList.spliterator();
Spliterator<Integer> half = split.trySplit();  // splits into two halves

// HashSet — decent (buckets)
// LinkedList — terrible (must traverse to find midpoint)
// TreeSet — decent (tree structure)

// characteristics() flags tell the stream engine what's safe
split.hasCharacteristics(Spliterator.SIZED);     // exact size known
split.hasCharacteristics(Spliterator.SUBSIZED);   // splits know their size
split.hasCharacteristics(Spliterator.ORDERED);    // encounter order matters
split.hasCharacteristics(Spliterator.IMMUTABLE);  // source won't change during traversal
split.hasCharacteristics(Spliterator.CONCURRENT); // source can be modified concurrently
```

---

## 11. Common Mistakes & Interview Traps

### 1. Stream can be consumed only once

```java
Stream<String> stream = list.stream();
stream.forEach(System.out::println);
stream.forEach(System.out::println);  // IllegalStateException!
```

### 2. Stateful lambdas in parallel streams

```java
// WRONG — non-deterministic, unsafe
Set<Integer> seen = new HashSet<>();
intStream.parallel().filter(n -> seen.add(n));  // Not associative, leaks state

// RIGHT — use distinct()
intStream.parallel().distinct();
```

### 3. Null-check in toMap

```java
// Throws NullPointerException if value is null
stream.collect(Collectors.toMap(k, v));  // v must be non-null

// Allow null values
stream.collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
```

### 4. Ordering guarantees in parallel

```java
// forEach order is NOT guaranteed in parallel
stream.parallel().forEach(System.out::println);  // random order

// forEachOrdered guarantees order (slower)
stream.parallel().forEachOrdered(System.out::println);  // preserves order
```

### 5. Empty stream + toMap = empty map (not exception)

```java
Map<Integer, String> empty = Stream.<Person>empty()
    .collect(Collectors.toMap(Person::id, Person::name));
// No exception — returns empty HashMap
```

### 6. groupBy on null key

```java
// groupBy does NOT tolerate null keys (unlike HashMap)
people.stream().collect(Collectors.groupingBy(p -> null));  // NullPointerException!
```

### 7. peek() for mutation — don't

```java
// peek is for debugging, not mutation
stream.peek(list::add);  // Never rely on peek for side effects — undefined behavior

// Use forEach as terminal op if you need side effects
stream.forEach(list::add);
```

---

## 12. Interview Q&A

**Q: What's the difference between `map` and `flatMap`?**
A: `map` is 1-to-1 (`T → R`). `flatMap` is 1-to-many (`T → Stream<R>`) and flattens nested streams.
Use `flatMap` when each element maps to a collection and you want all results in one flat stream.

**Q: When would `findFirst` vs `findAny` matter?**
A: `findFirst` guarantees the first element (respects order). `findAny` returns any element and is faster in parallel because it doesn't need ordering overhead. Default to `findAny` unless order matters.

**Q: Why does `toMap` throw on duplicate keys?**
A: Because `Map` requires unique keys. Pass a merge function as the third argument: `toMap(keyFn, valFn, (oldVal, newVal) -> newVal)`.

**Q: Is `parallelStream()` always faster?**
A: No. It adds overhead for splitting, thread scheduling, and merging. Small datasets (<1K), stateful operations, and I/O-bound tasks are slower with parallel streams. Always benchmark.

**Q: What happens if a stream is empty?**
A: `reduce(identity, acc)` returns identity. `reduce(acc)` returns `Optional.empty()`. `collect()` returns an empty container. `sum()` returns 0. `average()` returns `OptionalDouble.empty()`.

**Q: Can I reuse a stream?**
A: No. A stream is consumed after a terminal operation. Create a new supplier: `Supplier<Stream<T>> supplier = list::stream;`.

**Q: What's the difference between `collect(joining())` and `reduce("", String::concat)`?**
A: `joining()` uses `StringBuilder` internally — O(n). `reduce("", String::concat)` creates a new String every combine — O(n²). Always use `joining()` for string concatenation.
