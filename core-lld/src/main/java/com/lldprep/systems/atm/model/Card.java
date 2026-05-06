package com.lldprep.systems.atm.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Card {
    private final String cardNumber;
    private final String pinHash;
    private final List<String> accountIds;
    private final LocalDate expiryDate;
    private boolean blocked;

    public Card(String cardNumber, String pin, LocalDate expiryDate) {
        this.cardNumber = cardNumber;
        this.pinHash = hashPin(pin);
        this.accountIds = new ArrayList<>();
        this.expiryDate = expiryDate;
        this.blocked = false;
    }

    private String hashPin(String pin) {
        return String.valueOf(Objects.hash(pin));
    }

    public boolean validatePIN(String pin) {
        return pinHash.equals(hashPin(pin));
    }

    public void linkAccount(String accountId) {
        if (!accountIds.contains(accountId)) {
            accountIds.add(accountId);
        }
    }

    public void block() {
        this.blocked = true;
    }

    public void unblock() {
        this.blocked = false;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(expiryDate);
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public List<String> getAccountIds() {
        return Collections.unmodifiableList(accountIds);
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }
}
