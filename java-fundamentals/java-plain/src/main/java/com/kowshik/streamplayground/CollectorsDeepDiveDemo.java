package com.kowshik.streamplayground;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static java.util.stream.Collectors.*;

public class CollectorsDeepDiveDemo {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║              COLLECTORS DEEP DIVE DEMONSTRATION              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        // --- Test Data ---
        List<Person> people = buildPeople();
        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        part1_GroupingBy(people);
        part2_GroupingByDownstream(people);
        part3_MultiLevelGrouping(people);
        part4_PartitioningBy(numbers);
        part5_Teeing(numbers);
        part6_CollectingAndThen(numbers);
        part7_Joining(people);
        part8_Summarizing(people);
        part9_ToMapWithMerge(people);
        part10_FilteringAndMapping(people);
        part11_CustomCollector(numbers);
    }

    private static List<Person> buildPeople() {
        return List.of(
            new Person("Alice", "NYC", 30, Gender.FEMALE, 85000),
            new Person("Bob", "NYC", 25, Gender.MALE, 72000),
            new Person("Charlie", "NYC", 35, Gender.MALE, 95000),
            new Person("Diana", "SF", 28, Gender.FEMALE, 110000),
            new Person("Eve", "SF", 32, Gender.FEMALE, 105000),
            new Person("Frank", "SF", 40, Gender.MALE, 130000),
            new Person("Grace", "LA", 22, Gender.FEMALE, 65000),
            new Person("Hank", "LA", 45, Gender.MALE, 115000),
            new Person("Ivy", "LA", 27, Gender.FEMALE, 78000),
            new Person("Jack", "LA", 33, Gender.MALE, 92000)
        );
    }

    // ================================================================
    // PART 1: groupingBy — basic
    // ================================================================
    static void part1_GroupingBy(List<Person> people) {
        System.out.println("┌─ PART 1: groupingBy (basic) ─────────────────────────────────┐");

        Map<String, List<Person>> byCity = people.stream()
            .collect(groupingBy(Person::city));

        byCity.forEach((city, residents) ->
            System.out.println("  " + city + " → " + residents.stream()
                .map(Person::name).toList()));
        System.out.println();
    }

    // ================================================================
    // PART 2: groupingBy with downstream collectors
    // ================================================================
    static void part2_GroupingByDownstream(List<Person> people) {
        System.out.println("┌─ PART 2: groupingBy + downstream ────────────────────────────┐");

        // Count per city
        Map<String, Long> countByCity = people.stream()
            .collect(groupingBy(Person::city, counting()));
        System.out.println("  Count by city: " + countByCity);

        // Average salary by city
        Map<String, Double> avgSalaryByCity = people.stream()
            .collect(groupingBy(Person::city, averagingInt(Person::salary)));
        System.out.println("  Avg salary by city: " + avgSalaryByCity);

        // Total salary by city
        Map<String, Integer> totalSalaryByCity = people.stream()
            .collect(groupingBy(Person::city, summingInt(Person::salary)));
        System.out.println("  Total salary by city: " + totalSalaryByCity);

        // Max salary earner per city
        Map<String, Optional<Person>> topEarnerByCity = people.stream()
            .collect(groupingBy(Person::city, maxBy(Comparator.comparingInt(Person::salary))));
        topEarnerByCity.forEach((city, p) ->
            System.out.println("  Top earner in " + city + ": " + p.map(Person::name).orElse("none")));

        // Set of names per city (instead of List)
        Map<String, Set<String>> namesByCity = people.stream()
            .collect(groupingBy(Person::city, mapping(Person::name, toSet())));
        System.out.println("  Names by city (Set): " + namesByCity);
        System.out.println();
    }

    // ================================================================
    // PART 3: Multi-Level Grouping
    // ================================================================
    static void part3_MultiLevelGrouping(List<Person> people) {
        System.out.println("┌─ PART 3: Multi-Level Grouping ───────────────────────────────┐");

        Map<String, Map<Gender, List<Person>>> byCityThenGender = people.stream()
            .collect(groupingBy(Person::city, groupingBy(Person::gender)));

        byCityThenGender.forEach((city, byGender) -> {
            System.out.println("  " + city + ":");
            byGender.forEach((gender, persons) ->
                System.out.println("    " + gender + " → " + persons.stream()
                    .map(Person::name).toList()));
        });

        // Three-level: city → gender → salary average
        Map<String, Map<Gender, Double>> cityThenGenderThenAvgSalary = people.stream()
            .collect(groupingBy(Person::city,
                groupingBy(Person::gender, averagingInt(Person::salary))));
        System.out.println("  Avg salary by city then gender: " + cityThenGenderThenAvgSalary);
        System.out.println();
    }

    // ================================================================
    // PART 4: partitioningBy (special case of groupingBy)
    // ================================================================
    static void part4_PartitioningBy(List<Integer> numbers) {
        System.out.println("┌─ PART 4: partitioningBy ─────────────────────────────────────┐");
        System.out.println("  Input: " + numbers);

        Map<Boolean, List<Integer>> partition = numbers.stream()
            .collect(partitioningBy(n -> n % 2 == 0));
        System.out.println("  Even? → " + partition.get(true));
        System.out.println("  Odd?  → " + partition.get(false));

        // partition with downstream
        Map<Boolean, Long> countByParity = numbers.stream()
            .collect(partitioningBy(n -> n % 2 == 0, counting()));
        System.out.println("  Count by parity: " + countByParity);

        Map<Boolean, Optional<Integer>> maxByParity = numbers.stream()
            .collect(partitioningBy(n -> n % 2 == 0, maxBy(Comparator.naturalOrder())));
        System.out.println("  Max even: " + maxByParity.get(true).orElse(null));
        System.out.println("  Max odd:  " + maxByParity.get(false).orElse(null));
        System.out.println();
    }

    // ================================================================
    // PART 5: teeing (Java 12) — two collectors, merge results
    // ================================================================
    static void part5_Teeing(List<Integer> numbers) {
        System.out.println("┌─ PART 5: teeing (Java 12) — two collectors, merge results ───┐");
        System.out.println("  Input: " + numbers);

        // Count + sum in one pass
        record CountAndSum(long count, int sum) {}
        CountAndSum result = numbers.stream()
            .collect(teeing(
                counting(),
                summingInt(n -> n),
                CountAndSum::new
            ));
        System.out.println("  Count: " + result.count() + ", Sum: " + result.sum());

        // Min + max in one pass
        int realMin = numbers.stream().min(Comparator.naturalOrder()).orElse(0);
        int realMax = numbers.stream().max(Comparator.naturalOrder()).orElse(0);
        System.out.println("  Min: " + realMin + ", Max: " + realMax);
        System.out.println("  (teeing minBy + maxBy also works — explicit type witness: Collectors.<Integer, ...>teeing(...))");

        // Concept: teeing enables one-pass stats. Without it, you'd do multiple streams.
        System.out.println("  Key point: teeing merges 2 collectors into 1 pass — avoids re-streaming.");
        System.out.println();
    }

    // ================================================================
    // PART 6: collectingAndThen
    // ================================================================
    static void part6_CollectingAndThen(List<Integer> numbers) {
        System.out.println("┌─ PART 6: collectingAndThen ──────────────────────────────────┐");

        // Collect to list, then make immutable
        List<Integer> immutable = numbers.stream()
            .filter(n -> n > 5)
            .collect(collectingAndThen(toList(), Collections::unmodifiableList));
        System.out.println("  Immutable filtered list: " + immutable);

        // Collect to set, then get size
        int uniqueCount = Stream.of(1, 2, 2, 3, 3, 3)
            .collect(collectingAndThen(toSet(), Set::size));
        System.out.println("  Unique elements: " + uniqueCount);

        // Find max, then unwrap Optional
        int maxEven = numbers.stream()
            .filter(n -> n % 2 == 0)
            .collect(collectingAndThen(
                Collectors.<Integer>maxBy(Comparator.naturalOrder()),
                opt -> opt.orElseThrow(() -> new IllegalStateException("No even numbers"))
            ));
        System.out.println("  Max even (unwrapped): " + maxEven);
        System.out.println();
    }

    // ================================================================
    // PART 7: joining
    // ================================================================
    static void part7_Joining(List<Person> people) {
        System.out.println("┌─ PART 7: joining ────────────────────────────────────────────┐");

        String names = people.stream()
            .map(Person::name)
            .collect(joining(", "));
        System.out.println("  Names: " + names);

        String namesWithBrackets = people.stream()
            .map(Person::name)
            .collect(joining(", ", "[ ", " ]"));
        System.out.println("  With brackets: " + namesWithBrackets);

        // Why joining() is O(n) vs reduce("", String::concat) which is O(n²):
        System.out.println("  Note: joining() uses StringBuilder (O(n))");
        System.out.println("        reduce(\"\", String::concat) creates new String per element (O(n²))");
        System.out.println();
    }

    // ================================================================
    // PART 8: summarizing
    // ================================================================
    static void part8_Summarizing(List<Person> people) {
        System.out.println("┌─ PART 8: summarizingInt ─────────────────────────────────────┐");

        IntSummaryStatistics salaryStats = people.stream()
            .collect(summarizingInt(Person::salary));
        System.out.println("  Count: " + salaryStats.getCount());
        System.out.println("  Sum:   ₹" + salaryStats.getSum());
        System.out.println("  Min:   ₹" + salaryStats.getMin());
        System.out.println("  Avg:   ₹" + salaryStats.getAverage());
        System.out.println("  Max:   ₹" + salaryStats.getMax());

        // Without summarizing, you'd need 5 separate stream passes
        System.out.println("  (All 5 stats computed in a single stream pass)");
        System.out.println();
    }

    // ================================================================
    // PART 9: toMap with merge function
    // ================================================================
    static void part9_ToMapWithMerge(List<Person> people) {
        System.out.println("┌─ PART 9: toMap with merge function ──────────────────────────┐");

        // toMap throws on duplicate keys — use merge function
        Map<String, Person> byCity = people.stream()
            .collect(toMap(
                Person::city,
                p -> p,
                (existing, replacement) -> replacement  // Keep later
            ));
        System.out.println("  By city (keeps last): " + byCity.values().stream()
            .map(p -> p.name() + "(" + p.city() + ")").toList());

        // Alternative: pick max salary per city
        Map<String, Person> maxSalaryByCity = people.stream()
            .collect(toMap(
                Person::city,
                p -> p,
                (p1, p2) -> p1.salary() >= p2.salary() ? p1 : p2
            ));
        System.out.println("  Max salary per city:");
        maxSalaryByCity.forEach((city, p) ->
            System.out.println("    " + city + " → " + p.name() + " (₹" + p.salary() + ")"));

        // BinaryOperator.maxBy shorthand
        Map<String, Person> maxSalaryByCity2 = people.stream()
            .collect(toMap(
                Person::city,
                p -> p,
                BinaryOperator.maxBy(Comparator.comparingInt(Person::salary))
            ));
        System.out.println("  (Same, using BinaryOperator.maxBy)");
        System.out.println();
    }

    // ================================================================
    // PART 10: filtering & mapping collectors (Java 9)
    // ================================================================
    static void part10_FilteringAndMapping(List<Person> people) {
        System.out.println("┌─ PART 10: filtering() & mapping() downstream (Java 9) ───────┐");

        // Count high-earners per city using filtering() collector
        Map<String, Long> highEarnersByCity = people.stream()
            .collect(groupingBy(
                Person::city,
                filtering(p -> p.salary() > 90000, counting())
            ));
        System.out.println("  High earners (>90K) per city: " + highEarnersByCity);

        // Names of female employees per city using mapping() collector
        Map<String, List<String>> femaleNamesByCity = people.stream()
            .collect(groupingBy(
                Person::city,
                filtering(p -> p.gender() == Gender.FEMALE,
                    mapping(Person::name, toList()))
            ));
        System.out.println("  Female names per city: " + femaleNamesByCity);

        // flatMapping: each person → stream of skills, collect unique skills per city
        // (simulated — Person doesn't have skills, using name chars as example)
        Map<String, Set<Character>> firstCharsByCity = people.stream()
            .collect(groupingBy(
                Person::city,
                flatMapping(p -> p.name().chars().mapToObj(c -> (char) c),
                    toSet())
            ));
        System.out.println("  First chars by city (flatMapping demo):");
        firstCharsByCity.forEach((city, chars) ->
            System.out.println("    " + city + " → " + chars));
        System.out.println();
    }

    // ================================================================
    // PART 11: Custom Collector — Standard Deviation
    // ================================================================
    static void part11_CustomCollector(List<Integer> numbers) {
        System.out.println("┌─ PART 11: Custom Collector (Standard Deviation) ─────────────┐");
        System.out.println("  Input: " + numbers);

        // Mutable accumulator — records can't be mutated
        double stdDev = numbers.stream().collect(Collector.of(
            () -> new long[]{0, 0, 0},                  // [count, sum, sumSquares]
            (acc, val) -> {
                acc[0]++;                                // count
                acc[1] += val;                           // sum
                acc[2] += (long) val * val;              // sum of squares
            },
            (acc1, acc2) -> {
                acc1[0] += acc2[0];
                acc1[1] += acc2[1];
                acc1[2] += acc2[2];
                return acc1;
            },
            acc -> {
                double mean = (double) acc[1] / acc[0];
                double variance = (double) acc[2] / acc[0] - mean * mean;
                return Math.sqrt(Math.max(variance, 0));
            }
        ));

        // Verify with known formula
        double expectedStdDev = Math.sqrt(
            numbers.stream().mapToDouble(n -> n * n).average().orElse(0)
            - Math.pow(numbers.stream().mapToDouble(Integer::doubleValue).average().orElse(0), 2)
        );

        System.out.println("  Standard Deviation: " + stdDev);
        System.out.println("  Expected (two-pass): " + expectedStdDev);
        System.out.println("  Note: Collector.of() requires a MUTABLE accumulator — records won't work");
        System.out.println();
    }

    // ================================================================
    // Data Classes
    // ================================================================
    enum Gender { MALE, FEMALE }

    record Person(String name, String city, int age, Gender gender, int salary) {}
}
