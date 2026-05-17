package com.lldprep.systems.vendingmachine.service;

import com.lldprep.systems.vendingmachine.model.Product;
import com.lldprep.systems.vendingmachine.model.Slot;
import com.lldprep.systems.vendingmachine.model.enums.ProductCode;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages product inventory for all slots in the vending machine.
 * Thread-safe for inventory operations.
 */
public class ProductManager {
    private final Map<ProductCode, Slot> slots = new HashMap<>();

    public ProductManager() {
        initializeSlots();
    }

    private void initializeSlots() {
        for (ProductCode code : ProductCode.values()) {
            slots.put(code, new Slot(code, 10)); // Capacity: 10 items per slot
        }
    }

    /**
     * Load a product into a slot.
     */
    public void loadProduct(ProductCode code, Product product, int quantity) {
        Slot slot = slots.get(code);
        if (slot == null) {
            throw new IllegalArgumentException("Invalid slot: " + code);
        }
        slot.loadProduct(product, quantity);
    }

    /**
     * Get slot by code.
     */
    public Slot getSlot(ProductCode code) {
        return slots.get(code);
    }

    /**
     * Check if product is available at slot.
     */
    public boolean isAvailable(ProductCode code) {
        Slot slot = slots.get(code);
        return slot != null && slot.isAvailable();
    }

    /**
     * Get product at slot (if available).
     */
    public Product getProduct(ProductCode code) {
        Slot slot = slots.get(code);
        return slot != null ? slot.getProduct() : null;
    }

    /**
     * Dispense one unit from slot (decrements inventory).
     */
    public void dispenseOne(ProductCode code) {
        Slot slot = slots.get(code);
        if (slot == null) {
            throw new IllegalArgumentException("Invalid slot: " + code);
        }
        slot.dispenseOne();
    }

    /**
     * Get quantity available at slot.
     */
    public int getQuantity(ProductCode code) {
        Slot slot = slots.get(code);
        return slot != null ? slot.getQuantity() : 0;
    }

    /**
     * Get all slots (unmodifiable view).
     */
    public Map<ProductCode, Slot> getAllSlots() {
        return Collections.unmodifiableMap(slots);
    }

    /**
     * Get inventory summary for display.
     */
    public String getInventorySummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════════════════════════╗\n");
        sb.append("║              VENDING MACHINE INVENTORY                   ║\n");
        sb.append("╠══════════════════════════════════════════════════════════╣\n");

        for (ProductCode code : ProductCode.values()) {
            Slot slot = slots.get(code);
            String status;
            if (!slot.isAvailable()) {
                status = "EMPTY";
            } else {
                status = slot.getQuantity() + " in stock";
            }

            String productName = slot.getProduct() != null ? slot.getProduct().getName() : "Empty";
            BigDecimal price = slot.getProduct() != null ? slot.getProduct().getPrice() : null;
            String priceStr = price != null ? String.format("₹%s", price) : "-";

            sb.append(String.format("║  %s | %-12s | %6s | %-10s         ║%n",
                code, productName, priceStr, status));
        }

        sb.append("╚══════════════════════════════════════════════════════════╝");
        return sb.toString();
    }
}
