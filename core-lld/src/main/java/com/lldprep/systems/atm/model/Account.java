package com.lldprep.systems.atm.model;

import com.lldprep.systems.atm.model.enums.AccountType;

import java.math.BigDecimal;
import java.util.Objects;

public class Account {
    private final String accountId;
    private final AccountType type;
    private final String cardNumber;
    private BigDecimal balance;

    public Account(String accountId, AccountType type, String cardNumber, BigDecimal initialBalance) {
        this.accountId = accountId;
        this.type = type;
        this.cardNumber = cardNumber;
        this.balance = initialBalance;
    }

    public synchronized void debit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }
        balance = balance.subtract(amount);
    }

    public synchronized void credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        balance = balance.add(amount);
    }

    public synchronized BigDecimal getBalance() {
        return balance;
    }

    public String getAccountId() {
        return accountId;
    }

    public AccountType getType() {
        return type;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(accountId, account.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId);
    }
}
