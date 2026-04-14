package com.lldprep.foundations.behavioral.strategy.good;

import java.util.Arrays;

public class BubbleSortStrategy implements SortStrategy {

    @Override
    public int[] sort(int[] data) {
        int[] arr = Arrays.copyOf(data, data.length);
        for (int i = 0; i < arr.length - 1; i++)
            for (int j = 0; j < arr.length - i - 1; j++)
                if (arr[j] > arr[j + 1]) {
                    int tmp = arr[j]; arr[j] = arr[j + 1]; arr[j + 1] = tmp;
                }
        return arr;
    }

    @Override
    public String name() { return "BubbleSort"; }
}
