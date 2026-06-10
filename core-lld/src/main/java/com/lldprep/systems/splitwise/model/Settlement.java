package com.lldprep.systems.splitwise.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Settlement {
    private final String id;
    private final User payer;
    private final User payee;
    private final BigDecimal amount;
    private final LocalDateTime settledAt;

    public Settlement(User payer, User payee, BigDecimal amount) {
        this.id = "STL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.payer = payer;
        this.payee = payee;
        this.amount = amount;
        this.settledAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public User getPayer() {
        return payer;
    }

    public User getPayee() {
        return payee;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getSettledAt() {
        return settledAt;
    }

    @Override
    public String toString() {
        return payer.getName() + " -> " + payee.getName() + " : " + amount;
    }
}
