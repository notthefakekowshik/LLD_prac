package com.lldprep.orderbook.demo;

import com.lldprep.orderbook.model.MatchResult;
import com.lldprep.orderbook.model.Order;
import com.lldprep.orderbook.service.OrderBookEngine;
import com.lldprep.orderbook.service.TradeListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

/**
 * OrderBook Engine — Demo
 *
 * <p>Demonstrates all functional requirements:
 * <ol>
 *   <li>Basic limit-order matching</li>
 *   <li>Partial fills across multiple price levels</li>
 *   <li>Order cancellation</li>
 *   <li>Market orders</li>
 *   <li>Concurrent producers on two symbols — proving parallel, lock-free processing</li>
 * </ol>
 */
public class OrderBookDemo {

    public static void main(String[] args) throws InterruptedException {
        demo1_BasicMatch();
        demo2_PartialFill();
        demo3_Cancel();
        demo4_MarketOrder();
        demo5_ConcurrentProducers();
    }

    private static void demo1_BasicMatch() throws InterruptedException {
        section("Demo 1: Basic limit-order match");
        OrderBookEngine engine = new OrderBookEngine(loggingListener());

        await(engine.placeOrder(new Order("AAPL", Order.Side.SELL, Order.Type.LIMIT, 150.00, 10)));
        await(engine.placeOrder(new Order("AAPL", Order.Side.BUY,  Order.Type.LIMIT, 150.00, 10)));

        engine.shutdown();
    }

    private static void demo2_PartialFill() throws InterruptedException {
        section("Demo 2: Partial fill — buyer sweeps multiple ask levels");
        OrderBookEngine engine = new OrderBookEngine(loggingListener());

        await(engine.placeOrder(new Order("AAPL", Order.Side.SELL, Order.Type.LIMIT, 100.00, 5)));
        await(engine.placeOrder(new Order("AAPL", Order.Side.SELL, Order.Type.LIMIT, 101.00, 5)));
        await(engine.placeOrder(new Order("AAPL", Order.Side.SELL, Order.Type.LIMIT, 102.00, 5)));
        await(engine.placeOrder(new Order("AAPL", Order.Side.BUY,  Order.Type.LIMIT, 102.00, 15)));

        engine.shutdown();
    }

    private static void demo3_Cancel() throws InterruptedException {
        section("Demo 3: Order cancellation");
        OrderBookEngine engine = new OrderBookEngine(loggingListener());

        Order sellOrder = new Order("AAPL", Order.Side.SELL, Order.Type.LIMIT, 200.00, 10);
        await(engine.placeOrder(sellOrder));
        await(engine.cancelOrder("AAPL", sellOrder.getId()));
        await(engine.placeOrder(new Order("AAPL", Order.Side.BUY, Order.Type.LIMIT, 200.00, 10)));

        engine.shutdown();
    }

    private static void demo4_MarketOrder() throws InterruptedException {
        section("Demo 4: Market order");
        OrderBookEngine engine = new OrderBookEngine(loggingListener());

        await(engine.placeOrder(new Order("AAPL", Order.Side.SELL, Order.Type.LIMIT, 150.00, 5)));
        await(engine.placeOrder(new Order("AAPL", Order.Side.SELL, Order.Type.LIMIT, 151.00, 5)));
        await(engine.placeOrder(new Order("AAPL", Order.Side.BUY,  Order.Type.MARKET, 0, 8)));

        engine.shutdown();
    }

    private static void demo5_ConcurrentProducers() throws InterruptedException {
        section("Demo 5: Concurrent producers on AAPL and AMZN");
        OrderBookEngine engine = new OrderBookEngine(loggingListener());

        List<Thread> producers = new ArrayList<>();
        producers.add(new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                await(engine.placeOrder(new Order("AAPL", Order.Side.BUY, Order.Type.LIMIT, 140 + ThreadLocalRandom.current().nextInt(10), 1)));
            }
        }, "aapl-buyer"));
        producers.add(new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                await(engine.placeOrder(new Order("AAPL", Order.Side.SELL, Order.Type.LIMIT, 140 + ThreadLocalRandom.current().nextInt(10), 1)));
            }
        }, "aapl-seller"));
        producers.add(new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                await(engine.placeOrder(new Order("AMZN", Order.Side.BUY, Order.Type.LIMIT, 180 + ThreadLocalRandom.current().nextInt(10), 1)));
            }
        }, "amzn-buyer"));
        producers.add(new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                await(engine.placeOrder(new Order("AMZN", Order.Side.SELL, Order.Type.LIMIT, 180 + ThreadLocalRandom.current().nextInt(10), 1)));
            }
        }, "amzn-seller"));

        producers.forEach(Thread::start);
        for (Thread t : producers) {
            t.join();
        }

        engine.shutdown();
        System.out.println("  [All producers done — AAPL and AMZN ran on separate threads, zero shared locks]");
    }

    private static TradeListener loggingListener() {
        return new TradeListener() {
            @Override
            public void onTrade(MatchResult r) {
                System.out.println("  " + r);
            }

            @Override
            public void onOrderAccepted(Order o) {
                System.out.printf("  [RESTING]   %s%n", o);
            }

            @Override
            public void onOrderCancelled(Order o) {
                System.out.printf("  [CANCELLED] %s%n", o);
            }
        };
    }

    private static void await(Future<Void> f) {
        try {
            f.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void section(String title) {
        System.out.println("\n=== " + title + " ===");
    }
}
