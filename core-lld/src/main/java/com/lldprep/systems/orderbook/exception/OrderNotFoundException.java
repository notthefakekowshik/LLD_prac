package com.lldprep.orderbook.exception;

/**
 * Thrown when an operation (e.g., cancel) references an order ID that does not exist
 * in the book or has already been filled/cancelled.
 */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String orderId) {
        super("Order not found or already terminal: " + orderId);
    }
}
