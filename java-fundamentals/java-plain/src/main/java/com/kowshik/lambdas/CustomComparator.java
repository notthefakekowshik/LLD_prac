package com.kowshik.lambdas;

import java.util.*;

public class CustomComparator {
    static class Pair {
        int x, y;

        Pair(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public static void main(String[] args) {
        List<Pair> pairList = new ArrayList<>();
        pairList.add(new Pair(4, 5));
        pairList.add(new Pair(1, 2));
        pairList.add(new Pair(3, 1));
        pairList.add(new Pair(2, 3));
        pairList.add(new Pair(5, 4));
        pairList.add(new Pair(5, 2));

        Collections.sort(pairList, (a, b) -> {
            if (a.x == b.x) {
                return b.y - a.y;
            }
            return a.x - b.x;
        });

        Collections.sort(pairList, new Comparator<Pair>() {
            @Override
            public int compare(Pair o1, Pair o2) {
                if (o1.x == o2.x) {
                    return o2.y - o1.y;
                }
                return o1.x - o2.x;
            }
        });

        for (Pair p : pairList) {
            System.out.println(p.x + " -> " + p.y);
        }

    }
}
