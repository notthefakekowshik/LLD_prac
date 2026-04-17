package com.lldprep.systems.threadpool.future;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of CustomFuture that wraps a Callable task.
 * 
 * Thread-safe implementation using:
 * - AtomicReference for state transitions
 * - CountDownLatch for blocking get() operations
 * - volatile for visibility guarantees
 * 
 * State transitions:
 * NEW → RUNNING → COMPLETED (normal)
 * NEW → RUNNING → EXCEPTIONAL (exception thrown)
 * NEW → CANCELLED (cancelled before running)
 * RUNNING → CANCELLED (interrupted during execution)
 * 
 * @param <T> The result type
 */
public class FutureTask<T> implements CustomFuture<T>, Runnable {
    
    private enum State {
        NEW,
        RUNNING,
        COMPLETED,
        EXCEPTIONAL,
        CANCELLED
    }
    
    private final Callable<T> callable;
    private final AtomicReference<State> state;
    private final CountDownLatch latch;
    
    private volatile T result;
    private volatile Throwable exception;
    private volatile Thread runningThread;
    
    public FutureTask(Callable<T> callable) {
        if (callable == null) {
            throw new IllegalArgumentException("Callable cannot be null");
        }
        this.callable = callable;
        this.state = new AtomicReference<>(State.NEW);
        this.latch = new CountDownLatch(1);
    }
    
    public FutureTask(Runnable runnable, T result) {
        this(() -> {
            runnable.run();
            return result;
        });
    }
    
    @Override
    public void run() {
        if (!state.compareAndSet(State.NEW, State.RUNNING)) {
            return;
        }
        
        runningThread = Thread.currentThread();
        
        try {
            T computedResult = callable.call();
            
            if (state.compareAndSet(State.RUNNING, State.COMPLETED)) {
                this.result = computedResult;
            }
        } catch (Throwable t) {
            if (state.compareAndSet(State.RUNNING, State.EXCEPTIONAL)) {
                this.exception = t;
            }
        } finally {
            runningThread = null;
            latch.countDown();
        }
    }
    
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        while (true) {
            State currentState = state.get();
            
            if (currentState == State.COMPLETED || 
                currentState == State.EXCEPTIONAL || 
                currentState == State.CANCELLED) {
                return false;
            }
            
            if (state.compareAndSet(currentState, State.CANCELLED)) {
                if (mayInterruptIfRunning && currentState == State.RUNNING) {
                    Thread t = runningThread;
                    if (t != null) {
                        t.interrupt();
                    }
                }
                latch.countDown();
                return true;
            }
        }
    }
    
    @Override
    public boolean isCancelled() {
        return state.get() == State.CANCELLED;
    }
    
    @Override
    public boolean isDone() {
        State currentState = state.get();
        return currentState == State.COMPLETED || 
               currentState == State.EXCEPTIONAL || 
               currentState == State.CANCELLED;
    }
    
    @Override
    public T get() throws InterruptedException, ExecutionException {
        latch.await();
        return getResult();
    }
    
    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (!latch.await(timeout, unit)) {
            throw new TimeoutException("Timeout waiting for task completion");
        }
        return getResult();
    }
    
    private T getResult() throws ExecutionException {
        State currentState = state.get();
        
        switch (currentState) {
            case COMPLETED:
                return result;
            case EXCEPTIONAL:
                throw new ExecutionException(exception);
            case CANCELLED:
                throw new ExecutionException(new InterruptedException("Task was cancelled"));
            default:
                throw new IllegalStateException("Task is in unexpected state: " + currentState);
        }
    }
}
