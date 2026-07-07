package com.codereview.wallet;

public class Wallet {
    private String id;
    private String ownerName;
    private double balance;

    public Wallet(String id, String ownerName, double balance) {
        this.id = id;
        this.ownerName = ownerName;
        this.balance = balance;
    }

    public String getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Wallet wallet = (Wallet) o;
        return id.equals(wallet.id);
    }
}
