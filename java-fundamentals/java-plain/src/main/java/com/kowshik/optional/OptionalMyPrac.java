package com.kowshik.optional;

import java.sql.SQLOutput;
import java.util.Optional;

public class OptionalMyPrac {

    private static void test1_cache_hit_miss_call_db() {
        Optional<String> cacheHitGetUser = getUserByIdFromCache(1);
        Optional<String> cacheMissGetUser = getUserByIdFromCache(2);
        System.out.println("Cache hit: " + cacheHitGetUser.isPresent() );
        System.out.println("Cache hit: " + (cacheHitGetUser.orElse("N/A")));
        System.out.println();
        System.out.println("Cache miss: " + cacheMissGetUser.isPresent() );

        System.out.println("=".repeat(60));
        System.out.println("Hit DB when cache miss");
        cacheHitGetUser.ifPresentOrElse(
            user -> System.out.println("Found in cache: " + user),
            () -> {
                Optional<String> fromDb = getUserByIdFromDB(1);
                fromDb.ifPresent(u -> System.out.println("Fetched from DB: " + u));
            }
        );

        cacheMissGetUser.ifPresentOrElse(
            user -> System.out.println("Found in cache: " + user),
            () -> {
                Optional<String> fromDb = getUserByIdFromDB(2);
                fromDb.ifPresent(u -> System.out.println("Fetched from DB: " + u));
            }
        );


        String user = getUserByIdFromCache(2).or(() -> getUserByIdFromDB(2)).orElse("N/A");
        String user2 = getUserByIdFromCache(2).orElse(getUserByIdFromDB(2).orElse("N/A"));
        String user3 = getUserByIdFromCache(2).orElseGet(() -> getPlainString());

        System.out.println("user: " + user);
        System.out.println("user2: " + user2);
        System.out.println("user3: " + user3);
    }

    private static String getPlainString() {
        return "Plain string";
    }
    private static Optional<String> getUserByIdFromCache(int id) {
        if (id == 1) {
            return Optional.of("Elon");
        }
        return Optional.empty();
    }

    private static Optional<String> getUserByIdFromDB(int id) {
        return Optional.of("User-" + id);
    }

    public static void main(String[] args) {
        test1_cache_hit_miss_call_db();
    }
}
