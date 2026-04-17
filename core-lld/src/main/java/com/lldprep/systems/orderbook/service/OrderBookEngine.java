package com.lldprep.orderbook.service;

import com.lldprep.orderbook.model.Order;

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
        return CompletableFuture.runAsync(
                () -> bookFor(order.getSymbol()).placeOrder(order),
                executorFor(order.getSymbol()));
    }

    public Future<Void> cancelOrder(String symbol, String orderId) {
        return CompletableFuture.runAsync(
                () -> bookFor(symbol).cancelOrder(orderId),
                executorFor(symbol));
    }

    public void shutdown() {
        executors.values().forEach(ExecutorService::shutdown);
    }

    private ExecutorService executorFor(String symbol) {
        return executors.computeIfAbsent(symbol, s -> Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "orderbook-" + s);
            t.setDaemon(true);
            return t;
        }));
    }

    private OrderBook bookFor(String symbol) {
        return books.computeIfAbsent(symbol, s -> new OrderBook(s, listener));
    }
}
