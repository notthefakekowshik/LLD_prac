package com.lldprep.systems.vendingmachine.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Represents a product available in the vending machine.
 * Immutable after creation.
 */
public class Product {
    private final String sku;
    private final String name;
    private final BigDecimal price;

    public Product(String sku, String name, BigDecimal price) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU cannot be empty");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        this.sku = sku;
        this.name = name;
        this.price = price;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(sku, product.sku);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku);
    }

    @Override
    public String toString() {
        return name + " (" + sku + ") - ₹" + price;
    }
}
