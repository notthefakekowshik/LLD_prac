package com.lldprep.systems.moviebooking.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Payment {
    private final String paymentId;
    private final double amount;
    private final boolean success;
    private final LocalDateTime timestamp;

    public Payment(double amount, boolean success) {
        this.paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.amount = amount;
        this.success = success;
        this.timestamp = LocalDateTime.now();
    }

    public String getPaymentId() {
        return paymentId;
    }

    public double getAmount() {
        return amount;
    }

    public boolean isSuccess() {
        return success;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return paymentId + " | ₹" + amount + " | " + (success ? "SUCCESS" : "FAILED");
    }
}
