package com.lldprep.systems.vendingmachine.model;

import com.lldprep.systems.vendingmachine.model.enums.ProductCode;

import java.math.BigDecimal;

/**
 * A slot in the vending machine that holds a product and tracks quantity.
 * Thread-safe for inventory operations.
 */
public class Slot {
    private final ProductCode code;
    private Product product;
    private int quantity;
    private final int capacity;

    public Slot(ProductCode code, int capacity) {
        this.code = code;
        this.capacity = capacity;
        this.quantity = 0;
    }

    public synchronized boolean isAvailable() {
        return product != null && quantity > 0;
    }

    public synchronized boolean isEmpty() {
        return quantity == 0;
    }

    public synchronized void loadProduct(Product product, int count) {
        if (count < 0 || count > capacity) {
            throw new IllegalArgumentException("Invalid quantity: " + count);
        }
        this.product = product;
        this.quantity = count;
    }

    public synchronized void dispenseOne() {
        if (quantity <= 0) {
            throw new IllegalStateException("Slot " + code + " is empty");
        }
        quantity--;
    }

    public synchronized void restock(int count) {
        if (count < 0 || quantity + count > capacity) {
            throw new IllegalArgumentException("Cannot restock " + count + ". Current: " + quantity + ", Capacity: " + capacity);
        }
        quantity += count;
    }

    // Getters
    public ProductCode getCode() {
        return code;
    }

    public synchronized Product getProduct() {
        return product;
    }

    public synchronized int getQuantity() {
        return quantity;
    }

    public int getCapacity() {
        return capacity;
    }

    public synchronized BigDecimal getPrice() {
        return product != null ? product.getPrice() : null;
    }

    @Override
    public String toString() {
        return "Slot " + code + ": " + (product != null ? product.getName() : "Empty") + " x " + quantity;
    }
}
