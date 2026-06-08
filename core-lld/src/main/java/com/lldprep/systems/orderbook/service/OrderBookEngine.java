package com.lldprep.systems.orderbook.service;

import com.lldprep.systems.orderbook.model.Order;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Central engine that routes orders to the correct per-symbol order book.
 *
 * <p><b>Concurrency model — thread confinement:</b>
 * <pre>
 *   "AAPL" → SingleThreadExecutor-1  → OrderBook(AAPL)
 *   "AMZN" → SingleThreadExecutor-2  → OrderBook(AMZN)
 *   "TSLA" → SingleThreadExecutor-3  → OrderBook(TSLA)
 * </pre>
 * All mutations to an {@link OrderBook} happen on its dedicated thread.
 * Different symbols match in parallel across threads — no locking inside the book.
 */
public class OrderBookEngine {

    private final TradeListener listener;

    // CRITICAL SECTION — symbol → single-thread executor (ConcurrentHashMap for safe put-if-absent)
    private final ConcurrentHashMap<String, ExecutorService> executors = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, OrderBook> books = new ConcurrentHashMap<>();

    public OrderBookEngine(TradeListener listener) {
        this.listener = listener;
    }

    public Future<Void> placeOrder(Order order) {
        ExecutorService executorForCurrentSymbol = getExecutorForCurrentSymbol(order.getSymbol());
        OrderBook orderBookForCurrentSymbol = getBookForCurrentSymbol(order.getSymbol());
        return CompletableFuture.runAsync(
                () -> orderBookForCurrentSymbol.placeOrder(order),
            executorForCurrentSymbol);
    }

    public Future<Void> cancelOrder(String symbol, String orderId) {
        OrderBook orderBookForCurrentSymbol = getBookForCurrentSymbol(symbol);
        return CompletableFuture.runAsync(
                () -> orderBookForCurrentSymbol.cancelOrder(orderId),
                getExecutorForCurrentSymbol(symbol));
    }

    public void shutdown() {
        for(ExecutorService executorService : executors.values()) {
            executorService.shutdown();
        }
        // It's the same thing.
        executors.values().forEach(ExecutorService::shutdown);
    }

    private ExecutorService getExecutorForCurrentSymbol(String symbol) {
        return executors.computeIfAbsent(symbol, s -> Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "orderbook-" + s);
            t.setDaemon(true);
            return t;
        }));
    }

    private OrderBook getBookForCurrentSymbol(String symbol) {
        return books.computeIfAbsent(symbol, s -> new OrderBook(s, listener));
    }
}
