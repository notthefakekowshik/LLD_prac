package com.lldprep.foundations.structural.flyweight.good;

import java.util.HashMap;
import java.util.Map;

/**
 * Flyweight Factory — ensures only one {@link TreeType} instance exists per (name, color, texture) key.
 *
 * <p>This is the cache that makes the pattern work. Without it, callers could accidentally
 * create duplicate flyweights and lose all the memory savings.
 */
public class TreeTypeFactory {

    private static final Map<String, TreeType> cache = new HashMap<>();

    public static TreeType getTreeType(String name, String color, String texture) {
        String key = name + "|" + color + "|" + texture;
        return cache.computeIfAbsent(key, k -> {
            System.out.printf("  [Factory] Creating NEW TreeType: %s%n", key);
            return new TreeType(name, color, texture);
        });
    }

    public static int getCachedTypeCount() {
        return cache.size();
    }
}
