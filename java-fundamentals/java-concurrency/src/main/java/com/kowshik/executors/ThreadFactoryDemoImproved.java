package com.kowshik.executors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadFactoryDemoImproved {
    public static void main(String[] args) {
        // This is using custom thread pool, we can use OOTB executors if we want. It
        // all depends on our requirements.
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                10, 200, 1L, TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(500),
                new MyNameThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());

        for (int i = 1; i < 1000; i++) {
            threadPoolExecutor.submit(new AdditionTask(1, 2));
        }

        threadPoolExecutor.shutdown();

        System.out.println("OOTB executor");
        try (ExecutorService executorService = Executors.newFixedThreadPool(5,
                new MyNameThreadFactoryImproved("OOTB-pool-Thread-", false))) {
            for (int i = 1; i < 1000; i++) {
                executorService.submit(new AdditionTask(i, i));
            }
        }
    }
}
