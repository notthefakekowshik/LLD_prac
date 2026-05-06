package com.lldprep.systems.atm.model;

import com.lldprep.systems.atm.model.enums.TransactionStatus;
import com.lldprep.systems.atm.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Transaction {
    private final String transactionId;
    private final TransactionType type;
    private final BigDecimal amount;
    private final String accountId;
    private final LocalDateTime timestamp;
    private TransactionStatus status;
    private String description;

    public Transaction(TransactionType type, BigDecimal amount, String accountId) {
        this.transactionId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.type = type;
        this.amount = amount;
        this.accountId = accountId;
        this.timestamp = LocalDateTime.now();
        this.status = TransactionStatus.PENDING;
    }

    public void complete() {
        this.status = TransactionStatus.COMPLETED;
    }

    public void fail() {
        this.status = TransactionStatus.FAILED;
    }

    public void cancel() {
        this.status = TransactionStatus.CANCELLED;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getAccountId() {
        return accountId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }
}
