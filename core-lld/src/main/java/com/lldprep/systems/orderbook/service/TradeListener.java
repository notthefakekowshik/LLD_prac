package com.lldprep.orderbook.service;

import com.lldprep.orderbook.model.MatchResult;
import com.lldprep.orderbook.model.Order;

/**
 * Callback interface for order book events.
 *
 * <p>Implementations are called from the symbol's single-thread executor,
 * so they must be non-blocking (hand off to another thread if heavy work is needed).
 */
public interface TradeListener {

    /** Called every time a match (fill) occurs. */
    void onTrade(MatchResult result);

    /** Called when an order is accepted into the book (resting, not yet matched). */
    void onOrderAccepted(Order order);

    /** Called when an order is fully or partially cancelled. */
    void onOrderCancelled(Order order);
}
