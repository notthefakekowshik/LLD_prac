package com.kowshik.lambdas;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CustomComparatorCompareToMethod {
    public static void main(String[] args) {

        int val = Integer.compare(9, 1);
        System.out.println(val);

        int stringVal = "a".compareTo("c");
        System.out.println(stringVal);

        Comparator<String> cmp = (a, b) -> {
            int x = Integer.compare(b.length(), a.length());
            if (x != 0)
                return x;
            return a.compareTo(b);
        };

        List<String> list = Arrays.asList("bb", "aa", "c");

        Collections.sort(list, cmp);

        System.out.println(list);
    }
}
