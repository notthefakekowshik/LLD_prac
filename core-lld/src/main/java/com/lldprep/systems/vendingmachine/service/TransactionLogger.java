package com.lldprep.systems.vendingmachine.service;

import com.lldprep.systems.vendingmachine.model.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Logs all vending machine transactions for audit trail.
 * Thread-safe.
 */
public class TransactionLogger {
    private final List<Transaction> transactions = Collections.synchronizedList(new ArrayList<>());

    /**
     * Log a transaction.
     */
    public void log(Transaction transaction) {
        transactions.add(transaction);
    }

    /**
     * Get all transactions (unmodifiable view).
     */
    public List<Transaction> getAllTransactions() {
        return Collections.unmodifiableList(new ArrayList<>(transactions));
    }

    /**
     * Get transaction count.
     */
    public int getTransactionCount() {
        return transactions.size();
    }

    /**
     * Get successful sales count.
     */
    public long getSuccessfulSalesCount() {
        return transactions.stream()
            .filter(t -> t.getStatus() == Transaction.Status.SUCCESS)
            .count();
    }

    /**
     * Calculate total revenue from successful sales.
     */
    public java.math.BigDecimal getTotalRevenue() {
        return transactions.stream()
            .filter(t -> t.getStatus() == Transaction.Status.SUCCESS)
            .map(Transaction::getProductPrice)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    /**
     * Get recent transactions (last N).
     */
    public List<Transaction> getRecentTransactions(int count) {
        int start = Math.max(0, transactions.size() - count);
        return Collections.unmodifiableList(new ArrayList<>(transactions.subList(start, transactions.size())));
    }
}
