package com.lldprep.foundations.behavioral.strategy.bad;

import java.util.Arrays;

/**
 * BAD: Algorithm selection is hardcoded via if-else inside the class.
 *
 * Problems:
 * 1. OCP violation — adding a new sort requires editing this class.
 * 2. SRP violation — the sorter owns all algorithm implementations.
 * 3. Cannot swap algorithm at runtime (e.g., different strategy for small vs large arrays).
 * 4. Unit testing any single algorithm requires exercising the whole class.
 */
public class SorterBad {

    public int[] sort(int[] data, String algorithm) {
        int[] copy = Arrays.copyOf(data, data.length);

        if (algorithm.equals("bubble")) {
            for (int i = 0; i < copy.length - 1; i++)
                for (int j = 0; j < copy.length - i - 1; j++)
                    if (copy[j] > copy[j + 1]) {
                        int tmp = copy[j]; copy[j] = copy[j + 1]; copy[j + 1] = tmp;
                    }
        } else if (algorithm.equals("selection")) {
            for (int i = 0; i < copy.length - 1; i++) {
                int minIdx = i;
                for (int j = i + 1; j < copy.length; j++)
                    if (copy[j] < copy[minIdx]) minIdx = j;
                int tmp = copy[minIdx]; copy[minIdx] = copy[i]; copy[i] = tmp;
            }
        } else if (algorithm.equals("insertion")) {
            for (int i = 1; i < copy.length; i++) {
                int key = copy[i], j = i - 1;
                while (j >= 0 && copy[j] > key) { copy[j + 1] = copy[j]; j--; }
                copy[j + 1] = key;
            }
        }
        // Adding "merge" or "quick" → must edit this class. OCP violation.
        return copy;
    }
}
