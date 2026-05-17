package com.lldprep.systems.vendingmachine.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import com.lldprep.systems.vendingmachine.model.enums.Denomination;
import com.lldprep.systems.vendingmachine.model.enums.ProductCode;

/**
 * Immutable record of a vending machine transaction.
 */
public class Transaction {
    private final String transactionId;
    private final LocalDateTime timestamp;
    private final ProductCode slotCode;
    private final String productName;
    private final BigDecimal productPrice;
    private final BigDecimal amountInserted;
    private final BigDecimal changeReturned;
    private final Map<Denomination, Integer> changeBreakdown;
    private final Status status;

    public enum Status {
        SUCCESS, FAILED_INSUFFICIENT_FUNDS, FAILED_OUT_OF_STOCK, 
        FAILED_INSUFFICIENT_CHANGE, CANCELLED
    }

    private Transaction(Builder builder) {
        this.transactionId = builder.transactionId;
        this.timestamp = builder.timestamp;
        this.slotCode = builder.slotCode;
        this.productName = builder.productName;
        this.productPrice = builder.productPrice;
        this.amountInserted = builder.amountInserted;
        this.changeReturned = builder.changeReturned;
        this.changeBreakdown = builder.changeBreakdown != null 
            ? Collections.unmodifiableMap(builder.changeBreakdown) 
            : Collections.emptyMap();
        this.status = builder.status;
    }

    // Getters
    public String getTransactionId() { return transactionId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public ProductCode getSlotCode() { return slotCode; }
    public String getProductName() { return productName; }
    public BigDecimal getProductPrice() { return productPrice; }
    public BigDecimal getAmountInserted() { return amountInserted; }
    public BigDecimal getChangeReturned() { return changeReturned; }
    public Map<Denomination, Integer> getChangeBreakdown() { return changeBreakdown; }
    public Status getStatus() { return status; }

    @Override
    public String toString() {
        return String.format("Txn[%s] %s | %s @ %s | Inserted: ₹%s | Change: ₹%s | Status: %s",
            transactionId.substring(0, 8),
            timestamp.toLocalTime(),
            productName,
            slotCode,
            amountInserted,
            changeReturned,
            status
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String transactionId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        private LocalDateTime timestamp = LocalDateTime.now();
        private ProductCode slotCode;
        private String productName;
        private BigDecimal productPrice;
        private BigDecimal amountInserted;
        private BigDecimal changeReturned = BigDecimal.ZERO;
        private Map<Denomination, Integer> changeBreakdown;
        private Status status = Status.SUCCESS;

        public Builder slotCode(ProductCode slotCode) {
            this.slotCode = slotCode;
            return this;
        }

        public Builder productName(String productName) {
            this.productName = productName;
            return this;
        }

        public Builder productPrice(BigDecimal productPrice) {
            this.productPrice = productPrice;
            return this;
        }

        public Builder amountInserted(BigDecimal amountInserted) {
            this.amountInserted = amountInserted;
            return this;
        }

        public Builder changeReturned(BigDecimal changeReturned) {
            this.changeReturned = changeReturned;
            return this;
        }

        public Builder changeBreakdown(Map<Denomination, Integer> changeBreakdown) {
            this.changeBreakdown = changeBreakdown;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Transaction build() {
            return new Transaction(this);
        }
    }
}
