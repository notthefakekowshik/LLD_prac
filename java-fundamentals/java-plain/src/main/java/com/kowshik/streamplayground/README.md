# Stream Playground

Quick experiments with `java.util.stream`.

## Quick Run

```bash
cd /Users/knarayanam/backup_folder/MY_FILES/LLD_prac
mvn exec:java -Dexec.mainClass="com.kowshik.streamplayground.StreamBasicsDemo" -pl java-fundamentals/java-plain -am
```

## Topics to Explore

- **Intermediate ops**: `map`, `filter`, `flatMap`, `distinct`, `sorted`, `peek`
- **Terminal ops**: `collect`, `reduce`, `forEach`, `findFirst`, `anyMatch`
- **Collectors**: `toList`, `toSet`, `groupingBy`, `partitioningBy`, `joining`
- **Parallel streams**: when to use, when NOT to use
- **Primitive streams**: `IntStream`, `LongStream`, `DoubleStream`
- **Infinite streams**: `Stream.iterate`, `Stream.generate`
