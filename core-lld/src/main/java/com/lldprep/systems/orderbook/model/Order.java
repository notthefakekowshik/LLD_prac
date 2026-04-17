package com.lldprep.orderbook.model;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Immutable value object representing a single order placed by a participant.
 *
 * <p>Price-time priority requires two fields:
 * <ul>
 *   <li>{@code price}     — determines priority rank (higher bid / lower ask wins)</li>
 *   <li>{@code sequence}  — monotonically increasing; breaks ties within the same price (FIFO)</li>
 * </ul>
 *
 * {@code remainingQty} is mutable to support partial fills.
 */
public class Order {

    public enum Side   { BUY, SELL }
    public enum Type   { LIMIT, MARKET }
    public enum Status { OPEN, PARTIALLY_FILLED, FILLED, CANCELLED }

    private static final AtomicLong SEQ = new AtomicLong(0);

    private final String  id;
    private final String  symbol;
    private final Side    side;
    private final Type    type;
    private final double  price;        // ignored for MARKET orders
    private final Instant placedAt;
    private final long    sequence;     // global insertion order for time-priority

    private int    remainingQty;
    private Status status;

    public Order(String symbol, Side side, Type type, double price, int quantity) {
        this.id           = UUID.randomUUID().toString();
        this.symbol       = symbol;
        this.side         = side;
        this.type         = type;
        this.price        = price;
        this.remainingQty = quantity;
        this.status       = Status.OPEN;
        this.placedAt     = Instant.now();
        this.sequence     = SEQ.incrementAndGet();
    }

    // ----- mutations (called only from OrderBook — single thread) -----

    public void fill(int qty) {
        remainingQty -= qty;
        status = (remainingQty == 0) ? Status.FILLED : Status.PARTIALLY_FILLED;
    }

    public void cancel() {
        status = Status.CANCELLED;
    }

    // ----- accessors -----

    public String  getId()           { return id; }
    public String  getSymbol()       { return symbol; }
    public Side    getSide()         { return side; }
    public Type    getType()         { return type; }
    public double  getPrice()        { return price; }
    public int     getRemainingQty() { return remainingQty; }
    public Status  getStatus()       { return status; }
    public Instant getPlacedAt()     { return placedAt; }
    public long    getSequence()     { return sequence; }

    @Override
    public String toString() {
        return String.format("Order{id=%.8s, symbol=%s, side=%s, type=%s, price=%.2f, qty=%d, status=%s}",
                id, symbol, side, type, price, remainingQty, status);
    }
}
