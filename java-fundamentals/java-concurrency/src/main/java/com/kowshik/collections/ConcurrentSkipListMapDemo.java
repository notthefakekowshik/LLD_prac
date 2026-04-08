package com.kowshik.collections;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Demonstrates the usage of {@link java.util.concurrent.ConcurrentSkipListMap}.
 * <p>
 * <b>What is ConcurrentSkipListMap?</b>
 * <br>
 * It is a scalable concurrent implementation of
 * {@link java.util.concurrent.ConcurrentNavigableMap}.
 * The map is sorted according to the natural ordering of its keys, or by a
 * {@link java.util.Comparator}
 * provided at map creation time. It is thread-safe and allows multiple threads
 * to access and update
 * the map concurrently without external synchronization.
 * </p>
 *
 * <p>
 * <b>Internal Implementation:</b>
 * <br>
 * Internally, it is implemented using a <b>Skip List</b> data structure.
 * <ul>
 * <li>A Skip List is a probabilistic data structure that allows fast search
 * within an ordered sequence of elements.</li>
 * <li>It consists of multiple layers of linked lists. The bottom layer contains
 * all the elements.</li>
 * <li>Each higher layer acts as an "express lane" for the lists below,
 * containing a subset of the elements.</li>
 * <li>This structure allows for average time complexity of O(log n) for
 * contains, get, put and remove operations.</li>
 * <li>The implementation uses Lock-Free algorithms (CAS - Compare-And-Swap) to
 * ensure thread safety, making it highly efficient under high contention
 * compared to synchronized collections.</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>Real-life Use Cases:</b>
 * <ul>
 * <li><b>High-Frequency Trading:</b> Maintaining an order book where orders are
 * sorted by price and time. Multiple threads can add or match orders
 * concurrently.</li>
 * <li><b>Leaderboards:</b> Storing user scores in real-time gaming applications
 * where scores need to be constantly sorted and updated by many players.</li>
 * <li><b>Priority-based Task Scheduling:</b> Managing a queue of tasks where
 * execution order depends on priority (key) rather than insertion order, and
 * multiple producers/consumers access it.</li>
 * <li><b>Time-series Data:</b> Storing events ordered by timestamps in a
 * concurrent environment.</li>
 * </ul>
 * </p>
 */
public class ConcurrentSkipListMapDemo {
    public static void main(String[] args) {
        ConcurrentSkipListMap<String, String> map = new ConcurrentSkipListMap<>();
        map.put("one", "1");
        map.put("two", "2");
        map.put("three", "3");
        map.put("four", "4");
        map.put("five", "5");
        map.put("six", "6");
        map.put("seven", "7");
        map.put("eight", "8");
        map.put("nine", "9");
        map.put("ten", "10");

        String val = map.get("ten");
        System.out.println(val);

        TreeMap<Integer, String> treeMap = new TreeMap<>();
        treeMap.put(1, "one");
        treeMap.put(2, "two");
        treeMap.put(3, "three");
        treeMap.put(4, "four");
        treeMap.put(5, "five");
        treeMap.put(6, "six");
        treeMap.put(7, "seven");
        treeMap.put(8, "eight");
        treeMap.put(9, "nine");
        treeMap.put(10, "ten");

        SortedMap<Integer, String> subMapDemo = treeMap.subMap(3, 5);
        System.out.println(subMapDemo);
    }
}
