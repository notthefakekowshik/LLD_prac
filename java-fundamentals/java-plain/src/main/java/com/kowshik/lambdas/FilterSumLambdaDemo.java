package com.kowshik.lambdas;

import java.util.HashMap;

public class FilterSumLambdaDemo {

    public static void main(String[] args) {
        HashMap<String, Integer> map = new HashMap<>();
        map.put("kowshik-1", 2);
        map.put("kowshik-2", 1);
        map.put("not-kowshik-1", 1);

        int sum = map.entrySet()
                .stream()
                .filter(currEntry -> currEntry.getKey().startsWith("kowshik"))
                .mapToInt(currEntry -> currEntry.getValue())
                .sum();

        // mapToInt is required here because the stream operation
        // needs to be told to return an IntStream (which is a
        // stream of primitive int values) instead of a Stream of
        // Objects (which is the default return type of the map
        // method). This is so that the sum() method can be called on
        // the stream, which is only available for primitive types
        // such as int, long, double, etc.


        int sum_again = map.entrySet()
                        .stream().filter(currEntry -> currEntry.getKey().startsWith("kowshik"))
                        .map(currValue -> currValue.getValue())
                        .reduce(0, (a,b) -> (a+b));

        System.out.println(sum + " " + sum_again);
    }

}
