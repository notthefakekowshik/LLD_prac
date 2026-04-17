package com.lldprep.systems.orderbook.service;

import com.lldprep.systems.orderbook.model.MatchResult;
import com.lldprep.systems.orderbook.model.Order;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Per-symbol order book — runs entirely on one dedicated thread (thread confinement).
 * Uses TreeMap of Deques for price-time priority: O(log n) price access + FIFO within level.
 */
public class OrderBook {

    private final String symbol;
    private final TradeListener listener;

    /**
     * Bids organized by price (descending) and time (FIFO within price level).
     * TreeMap chosen over PriorityQueue because:
     * - O(log n) removal by price level (needed for order cancellations)
     * - O(1) access to best bid via firstEntry()
     * - Maintains sorted order across all price levels
     * - PriorityQueue lacks efficient removal of arbitrary elements
     */
    private final NavigableMap<Double, Deque<Order>> bids = new TreeMap<>(Collections.reverseOrder());
    private final NavigableMap<Double, Deque<Order>> asks = new TreeMap<>();
    private final Map<String, Order> allOrders = new HashMap<>();

    public OrderBook(String symbol, TradeListener listener) {
        this.symbol = symbol;
        this.listener = listener;
    }

    public void placeOrder(Order order) {
        allOrders.put(order.getId(), order);
        if (order.getType() == Order.Type.MARKET) {
            matchMarket(order);
        } else {
            matchLimit(order);
        }
    }

    public void cancelOrder(String orderId) {
        Order order = allOrders.get(orderId);
        if (order == null
                || order.getStatus() == Order.Status.FILLED
                || order.getStatus() == Order.Status.CANCELLED) {
            System.out.printf("  [CANCEL IGNORED] orderId=%.8s%n", orderId);
            return;
        }
        removeFromBook(order);
        order.cancel();
        listener.onOrderCancelled(order);
    }

    private void matchLimit(Order incoming) {
        if (incoming.getSide() == Order.Side.BUY) {
            matchBuyAgainstAsks(incoming);
            if (incoming.getRemainingQty() > 0) {
                addToBook(bids, incoming.getPrice(), incoming);
                listener.onOrderAccepted(incoming);
            }
        } else {
            matchSellAgainstBids(incoming);
            if (incoming.getRemainingQty() > 0) {
                addToBook(asks, incoming.getPrice(), incoming);
                listener.onOrderAccepted(incoming);
            }
        }
    }

    private void matchMarket(Order incoming) {
        if (incoming.getSide() == Order.Side.BUY) {
            matchBuyAgainstAsks(incoming);
        } else {
            matchSellAgainstBids(incoming);
        }
        if (incoming.getRemainingQty() > 0) {
            System.out.printf("  [MARKET UNFILLED] %d qty discarded%n", incoming.getRemainingQty());
        }
    }

    private void matchBuyAgainstAsks(Order buy) {
        while (buy.getRemainingQty() > 0 && !asks.isEmpty()) {
            Map.Entry<Double, Deque<Order>> entry = asks.firstEntry();
            double bestAskPrice = entry.getKey();

            if (buy.getType() == Order.Type.LIMIT && buy.getPrice() < bestAskPrice) {
                break;
            }

            Order resting = entry.getValue().peekFirst();
            int fillQty = Math.min(buy.getRemainingQty(), resting.getRemainingQty());

            buy.fill(fillQty);
            resting.fill(fillQty);
            listener.onTrade(new MatchResult(symbol, buy.getId(), resting.getId(), bestAskPrice, fillQty));

            if (resting.getRemainingQty() == 0) {
                entry.getValue().pollFirst();
                if (entry.getValue().isEmpty()) {
                    asks.pollFirstEntry();
                }
            }
        }
    }

    private void matchSellAgainstBids(Order sell) {
        while (sell.getRemainingQty() > 0 && !bids.isEmpty()) {
            Map.Entry<Double, Deque<Order>> entry = bids.firstEntry();
            double bestBidPrice = entry.getKey();

            if (sell.getType() == Order.Type.LIMIT && sell.getPrice() > bestBidPrice) {
                break;
            }

            Order resting = entry.getValue().peekFirst();
            int fillQty = Math.min(sell.getRemainingQty(), resting.getRemainingQty());

            sell.fill(fillQty);
            resting.fill(fillQty);
            listener.onTrade(new MatchResult(symbol, resting.getId(), sell.getId(), bestBidPrice, fillQty));

            if (resting.getRemainingQty() == 0) {
                entry.getValue().pollFirst();
                if (entry.getValue().isEmpty()) {
                    bids.pollFirstEntry();
                }
            }
        }
    }

    private void addToBook(NavigableMap<Double, Deque<Order>> side, double price, Order order) {
        side.computeIfAbsent(price, k -> new ArrayDeque<>()).addLast(order);
    }

    private void removeFromBook(Order order) {
        NavigableMap<Double, Deque<Order>> side = (order.getSide() == Order.Side.BUY) ? bids : asks;
        Deque<Order> level = side.get(order.getPrice());
        if (level != null) {
            level.remove(order);
            if (level.isEmpty()) {
                side.remove(order.getPrice());
            }
        }
    }
}
