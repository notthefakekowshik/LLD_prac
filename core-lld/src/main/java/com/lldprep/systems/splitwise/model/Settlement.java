package com.lldprep.systems.splitwise.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Settlement {
    private final String id;
    private final User sender;
    private final User receiver;
    private final BigDecimal amount;
    private final LocalDateTime settledAt;

    public Settlement(User sender, User receiver, BigDecimal amount) {
        this.id = "STL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.settledAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public User getSender() {
        return sender;
    }

    public User getReceiver() {
        return receiver;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getSettledAt() {
        return settledAt;
    }

    @Override
    public String toString() {
        return sender.getName() + " -> " + receiver.getName() + " : " + amount;
    }
}
