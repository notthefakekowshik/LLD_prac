package com.kowshik.lambdas.functional_interfaces;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Predicate<T> (Rule)
 * Method: boolean test(T t)
 * Signature: T -> boolean
 * Purpose: Used for filtering or conditional checks.
 */
public class PredicateDemo {
    public static void main(String[] args) {
        List<String> names = List.of("Kowshik", "Java", "Lambda", "Spring", "SAM");

        // 1. Basic Predicate
        Predicate<String> startsWithK = name -> name.startsWith("K");
        System.out.println("Starts with K (Kowshik): " + startsWithK.test("Kowshik"));

        // 2. Predicate for filtering
        Predicate<String> lengthGreaterThan4 = name -> name.length() > 4;

        // 3. Composing Predicates (and, or, negate)
        Predicate<String> complexRule = startsWithK.or(lengthGreaterThan4);

        List<String> filteredNames = names.stream()
                .filter(complexRule)
                .collect(Collectors.toList());

        System.out.println("Names starting with K or length > 4: " + filteredNames);
        
        // 4. Negate
        Predicate<String> notStartingWithK = startsWithK.negate();
        System.out.println("Names not starting with K: " + 
            names.stream().filter(notStartingWithK).collect(Collectors.toList()));
    }
}
