package com.kowshik.collectionsexperiments;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HashMapComputeIfAbsentExperiment {
    public static void main(String[] args) {
        Map<Integer, Integer> map = new ConcurrentHashMap<>();
        map.computeIfAbsent(1, k -> map.get(1) + 1);

        if (!map.containsKey(1)) {
            map.put(1, 1);
        }
    }

    private static Integer extracted() {
        return new Integer(1);
    }

}
