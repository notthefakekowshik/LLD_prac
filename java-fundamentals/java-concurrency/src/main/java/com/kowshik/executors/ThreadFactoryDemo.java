package com.kowshik.executors;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadFactoryDemo {
    public static void main(String[] args) {
        MyNameThreadFactory myNameThreadFactory = new MyNameThreadFactory();

        Thread k1 = myNameThreadFactory.newThread(new AdditionTask(1, 2));
        Thread k2 = myNameThreadFactory.newThread(new AdditionTask(2, 3));
        Thread k3 = myNameThreadFactory.newThread(new AdditionTask(3, 4));

        k1.start();
        k2.start();
        k3.start();

        // imagine the pain of using start() for a 1000 threads if they are present
        // see @ThreadFactoryDemoImproved
    }
}

class AdditionTask implements Runnable {

    private int x;
    private int y;

    AdditionTask(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void run() {
        int summed = x + y;
        System.out.println(
                "[ " + Thread.currentThread().getName() + " ] Addition of " + x + " + " + y + " y is " + summed);
    }
}

class MyNameThreadFactory implements ThreadFactory {

    AtomicInteger currentThreadCount = new AtomicInteger(0);

    @Override
    public Thread newThread(Runnable r) {
        Thread someThread = new Thread(r);
        someThread.setName("kowshik-thread-" + currentThreadCount.incrementAndGet());

        return someThread;
    }
}

class MyNameThreadFactoryImproved implements ThreadFactory {
    private String threadNamePrefix;
    private boolean isThreadDaemon;
    AtomicInteger currentThreadCount = new AtomicInteger(0);

    public MyNameThreadFactoryImproved(String threadNamePrefix, boolean isThreadDaemon) {
        this.threadNamePrefix = threadNamePrefix;
        this.isThreadDaemon = isThreadDaemon;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread someThread = new Thread(r);
        someThread.setDaemon(isThreadDaemon);
        someThread.setName((this.threadNamePrefix + currentThreadCount.incrementAndGet()));
        return someThread;
    }

}