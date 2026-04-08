package com.kowshik.collectionsexperiments;

import java.security.KeyStore.Entry;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class TreeMapDemo {
    public static void main(String[] args) {
        NavigableMap<Integer, String> treeMap = new TreeMap<Integer, String>();
        treeMap.put(5, "Five");
        treeMap.put(1, "One");
        treeMap.put(2, "Two");
        treeMap.put(3, "Three");
        treeMap.put(4, "Four");
        treeMap.put(10, "Ten");
        treeMap.put(6, "Six");
        treeMap.put(7, "Seven");
        treeMap.put(8, "Eight");
        treeMap.put(9, "Nine");
        System.out.println(treeMap);

        treeMap.lastKey();
        NavigableMap<Integer, String> someMap = treeMap.descendingMap();

        System.out.println(someMap);

        System.out.println("=========");

        // Returns a reverse order NavigableSet view of the keys contained in this map.
        // The iterator for this set returns the keys in descending order.
        // The set is backed by the map, so changes to the map are reflected in the set,
        // and vice-versa.
        for (Integer i : treeMap.descendingKeySet()) {
            System.out.println(i + " : " + treeMap.get(i));
        }

        System.out.println("=========");
        // it doesn't make any sense to have ascendingKeySet(), you're an idiot if you
        // think you need it.
        for (Integer i : treeMap.keySet()) {
            System.out.println(i + " : " + treeMap.get(i));
        }

        Map.Entry<Integer, String> entry = null;

        for (Map.Entry<Integer, String> currEntry : treeMap.entrySet()) {
            if (currEntry.getKey() == 5) {
                entry = currEntry;
                break;
            }
        }

        System.out.println("Current entry is " + entry.getKey() + " : " + entry.getValue());
        System.out.println("Successor of current entry is " + treeMap.higherEntry(entry.getKey()));
        System.out.println("Predecessor of current entry is " + treeMap.lowerEntry(entry.getKey()));

    }
}
