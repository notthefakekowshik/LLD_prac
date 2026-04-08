package com.kowshik.lambdas.functional_interfaces;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Function<T, R> (Transformation)
 * Method: R apply(T t)
 * Signature: T -> R
 * Purpose: Transforms an input of type T to an output of type R.
 */
public class FunctionDemo {
    public static void main(String[] args) {
        List<String> userEmails = List.of("alice@gmail.com", "bob@outlook.com", "charlie@gmail.com");

        // 1. Basic Function: Extract Domain
        Function<String, String> extractDomain = email -> email.split("@")[1];
        System.out.println("Extracted domain: " + extractDomain.apply("test@google.com"));

        // 2. Chaining Functions (andThen, compose)
        Function<String, String> toUpperCase = String::toUpperCase;
        Function<String, String> decorate = s -> "DOMAIN: " + s;

        // andThen: extractDomain -> toUpperCase -> decorate
        Function<String, String> domainPipeline = extractDomain
                .andThen(toUpperCase)
                .andThen(decorate);

        System.out.println("\n=== Applied Transformation Pipeline ===");
        List<String> domains = userEmails.stream()
                .map(domainPipeline)
                .collect(Collectors.toList());

        domains.forEach(System.out::println);

        // 3. compose: (Function passed to compose runs FIRST)
        Function<String, String> combined = decorate.compose(toUpperCase);
        System.out.println("\nCompose example: " + combined.apply("kowshik"));
    }
}
