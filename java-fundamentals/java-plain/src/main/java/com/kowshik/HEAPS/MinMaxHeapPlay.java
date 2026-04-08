package com.kowshik.HEAPS;

import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;

class Pair {
    int first;
    int second;

    Pair(int first, int second) {
        this.first = first;
        this.second = second;
    }
}

public class MinMaxHeapPlay {
    public static void main(String[] args) {
        // Comparator<Pair> pqSortByKeyComparator = new Comparator<Pair>() {
        // @Override
        // public int compare(Pair a, Pair b) {
        // if (a.first == b.first) {
        // return b.first - a.first;
        // }
        // return a.first - b.first;
        // }
        // };

        // This is cooooler way.
        Comparator<Pair> pqSortByKeyComparator = Comparator.comparingInt((Pair p) -> p.first)
                .thenComparingInt(p -> -p.second);

        PriorityQueue<Integer> minHeap = new PriorityQueue<>();
        minHeap.add(5);
        minHeap.add(7);
        minHeap.add(6);
        minHeap.add(1);

        System.out.println("Min Heap : " + minHeap);

        PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
        maxHeap.add(5);
        maxHeap.add(7);
        maxHeap.add(6);
        maxHeap.add(1);

        System.out.println("Max Heap : " + maxHeap);

        // PriorityQueue<Pair> pqSortByKeyByLambda = new PriorityQueue<>((a, b) -> {
        // if (a.first == b.first) {
        // return b.first - a.first;
        // }
        // return a.first - b.first;
        // });

        PriorityQueue<Pair> pqSortByKeyByLambda = new PriorityQueue<>(Comparator.comparingInt((Pair p) -> p.first)
                .thenComparingInt(p -> -p.second));
        pqSortByKeyByLambda.add(new Pair(2, 1));
        pqSortByKeyByLambda.add(new Pair(2, 5));
        pqSortByKeyByLambda.add(new Pair(1, 2));
        pqSortByKeyByLambda.add(new Pair(5, 2));
        pqSortByKeyByLambda.add(new Pair(0, 2));

        System.out.println("PQ sort by key by lambda ");
        while (!pqSortByKeyByLambda.isEmpty()) {
            System.out.println(pqSortByKeyByLambda.peek().first + " " + pqSortByKeyByLambda.peek().second);
            pqSortByKeyByLambda.poll();
        }

        PriorityQueue<Pair> pqSortByKeyByComparator = new PriorityQueue<>(pqSortByKeyComparator);
        pqSortByKeyByComparator.add(new Pair(2, 1));
        pqSortByKeyByComparator.add(new Pair(2, 5));
        pqSortByKeyByComparator.add(new Pair(1, 2));
        pqSortByKeyByComparator.add(new Pair(5, 2));
        pqSortByKeyByComparator.add(new Pair(0, 2));

        System.out.println("PQ sort by key by comparator ");
        while (!pqSortByKeyByComparator.isEmpty()) {
            System.out.println(pqSortByKeyByComparator.peek().first + " " + pqSortByKeyByComparator.peek().second);
            pqSortByKeyByComparator.poll();
        }

        System.out.println("====================");
        PriorityQueue<Integer> testingHeapWithDups = new PriorityQueue<>();
        testingHeapWithDups.add(1);
        testingHeapWithDups.add(1);
        System.out.println(testingHeapWithDups.size());
    }
}
