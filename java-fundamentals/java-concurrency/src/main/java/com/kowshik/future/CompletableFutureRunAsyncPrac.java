package com.kowshik.future;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CompletableFutureRunAsyncPrac {
    public static void main(String[] args) {
        ExecutorService ex = Executors.newFixedThreadPool(5);
        CompletableFuture.runAsync(() -> {
            System.out.println("Some printing async " + Thread.currentThread().getName());
        }, ex);

        System.out.println("main block " + Thread.currentThread().getName());

    }
}
