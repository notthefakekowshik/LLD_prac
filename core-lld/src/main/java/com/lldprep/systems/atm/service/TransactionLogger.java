package com.lldprep.systems.atm.service;

import com.lldprep.systems.atm.model.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class TransactionLogger {
    private final List<Transaction> transactions = new CopyOnWriteArrayList<>();

    public void log(Transaction transaction) {
        transactions.add(transaction);
        System.out.println("[AUDIT] " + formatTransaction(transaction));
    }

    public List<Transaction> getTransactionHistory(String accountId) {
        return transactions.stream()
            .filter(t -> t.getAccountId().equals(accountId))
            .collect(Collectors.toList());
    }

    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions);
    }

    private String formatTransaction(Transaction t) {
        return String.format("Txn[%s] %s | Account: %s | Amount: ₹%s | Status: %s | Time: %s",
            t.getTransactionId(),
            t.getType(),
            t.getAccountId(),
            t.getAmount(),
            t.getStatus(),
            t.getTimestamp()
        );
    }
}
