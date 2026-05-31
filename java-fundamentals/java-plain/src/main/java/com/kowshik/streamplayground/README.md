# Stream Playground

Deep dive into `java.util.stream` — collectors, parallel streams, primitive streams, infinite streams, and performance.

## Quick Run

```bash
cd /Volumes/Crucial_X9/LLD_prep

# Collectors: groupingBy, partitioningBy, teeing, summarizing, toMap, custom collector
mvn exec:java -Dexec.mainClass="com.kowshik.streamplayground.CollectorsDeepDiveDemo" -pl java-fundamentals/java-plain -q

# Parallel: when it wins/loses, shared state bug, findFirst vs findAny, LinkedList trap
mvn exec:java -Dexec.mainClass="com.kowshik.streamplayground.ParallelStreamDeepDiveDemo" -pl java-fundamentals/java-plain -q

# Primitives + Infinite: IntStream, boxing cost, iterate vs generate, takeWhile/dropWhile, reduce
mvn exec:java -Dexec.mainClass="com.kowshik.streamplayground.PrimitiveStreamsAndInfiniteDemo" -pl java-fundamentals/java-plain -q
```

## Files

| File | What it covers |
|------|---------------|
| `Streams_Theory.md` | Complete theory: creation, intermediate ops, terminal ops, collectors, primitive streams, parallel, infinte, reduce, spliterator, common mistakes, interview Q&A |
| `StreamBasicsDemo.java` | Quick warmup: filter, map, reduce, groupBy, flatMap, infinite streams (existing) |
| `CollectorsDeepDiveDemo.java` | groupingBy + downstream, multi-level, partitioningBy, teeing, collectingAndThen, joining, summarizingInt, toMap with merge, filtering/mapping (Java 9), custom Collector.of() |
| `ParallelStreamDeepDiveDemo.java` | When parallel wins/loses, shared mutable state bug, findFirst vs findAny, forEach vs forEachOrdered, reduce combiner, LinkedList split trap |
| `PrimitiveStreamsAndInfiniteDemo.java` | IntStream/LongStream/DoubleStream creation, summaryStatistics, boxing cost, stream type conversions, iterate Java 8 vs 9, generate, takeWhile/dropWhile, flatMap vs map |

## Topics Covered

- **Intermediate ops**: `map`, `filter`, `flatMap`, `distinct`, `sorted`, `peek`, `takeWhile`, `dropWhile`
- **Terminal ops**: `collect`, `reduce`, `sum`, `count`, `forEach`, `findFirst`, `findAny`, `anyMatch`, `allMatch`, `noneMatch`
- **Collectors**: `toList`, `toSet`, `toMap` (with merge), `groupingBy` (basic + downstream + multi-level), `partitioningBy`, `teeing`, `joining`, `summarizingInt`, `averagingInt`, `summingInt`, `maxBy`, `minBy`, `collectingAndThen`, `filtering` (Java 9), `mapping` (Java 9), `flatMapping`, `Collector.of()` custom
- **Parallel streams**: when to use/not, shared mutable state bug, `findFirst` vs `findAny`, `forEach` vs `forEachOrdered`, `reduce` combiner, `LinkedList` split trap, `ForkJoinPool.commonPool()`
- **Primitive streams**: `IntStream`, `LongStream`, `DoubleStream`, `range`/`rangeClosed`, `summaryStatistics`, boxing overhead, conversions (`mapToObj`, `boxed`, `mapToInt`, `flatMapToInt`)
- **Infinite streams**: `Stream.iterate` (Java 8 vs Java 9 with predicate), `Stream.generate`, Fibonacci
- **reduce() master operation**: identity/accumulator/combiner, associativity, parallel vs sequential
