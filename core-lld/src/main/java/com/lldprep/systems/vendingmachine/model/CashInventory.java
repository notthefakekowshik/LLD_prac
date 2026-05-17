package com.lldprep.systems.vendingmachine.model;

import com.lldprep.systems.vendingmachine.model.enums.Denomination;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages cash inventory for the vending machine.
 * Used for accepting payments and returning change.
 * Thread-safe.
 */
public class CashInventory {
    private final Map<Denomination, Integer> inventory = new ConcurrentHashMap<>();

    public CashInventory() {
        // Initialize with zero counts
        for (Denomination d : Denomination.values()) {
            inventory.put(d, 0);
        }
    }

    /**
     * Add cash to inventory.
     */
    public synchronized void addCash(Denomination denomination, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Cannot add negative count");
        }
        inventory.merge(denomination, count, Integer::sum);
    }

    /**
     * Remove cash from inventory (for giving change).
     * @return true if removal was successful
     */
    public synchronized boolean removeCash(Denomination denomination, int count) {
        if (count < 0) {
            return false;
        }
        int current = inventory.getOrDefault(denomination, 0);
        if (current < count) {
            return false;
        }
        inventory.put(denomination, current - count);
        return true;
    }

    /**
     * Check if we have enough of a specific denomination.
     */
    public boolean hasDenomination(Denomination denomination, int count) {
        return inventory.getOrDefault(denomination, 0) >= count;
    }

    /**
     * Get count of a specific denomination.
     */
    public int getCount(Denomination denomination) {
        return inventory.getOrDefault(denomination, 0);
    }

    /**
     * Calculate total cash value in inventory.
     */
    public synchronized BigDecimal getTotalValue() {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Denomination, Integer> entry : inventory.entrySet()) {
            BigDecimal value = BigDecimal.valueOf(entry.getKey().getValue());
            total = total.add(value.multiply(BigDecimal.valueOf(entry.getValue())));
        }
        return total;
    }

    /**
     * Get immutable view of inventory.
     */
    public Map<Denomination, Integer> getInventorySnapshot() {
        return Collections.unmodifiableMap(new HashMap<>(inventory));
    }

    /**
     * Check if machine can give exact change for an amount.
     * Uses greedy algorithm (works for Indian currency).
     */
    public synchronized boolean canGiveChange(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        int remaining = amount.intValue();

        // Work on a copy to not modify actual inventory
        Map<Denomination, Integer> temp = new HashMap<>(inventory);

        // Try denominations from highest to lowest
        Denomination[] denominations = Denomination.values();
        for (int i = denominations.length - 1; i >= 0; i--) {
            Denomination d = denominations[i];
            int value = d.getValue();
            int available = temp.getOrDefault(d, 0);
            int needed = remaining / value;
            int toUse = Math.min(needed, available);
            remaining -= toUse * value;

            if (remaining == 0) {
                return true;
            }
        }

        return remaining == 0;
    }

    /**
     * Calculate change to return.
     * @return Map of denomination -> count, or null if exact change not possible
     */
    public synchronized Map<Denomination, Integer> calculateChange(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Collections.emptyMap();
        }
        int remaining = amount.intValue();
        Map<Denomination, Integer> change = new HashMap<>();

        // Work on a copy to validate before committing
        Map<Denomination, Integer> temp = new HashMap<>(inventory);

        // Try denominations from highest to lowest (greedy)
        Denomination[] denominations = Denomination.values();
        for (int i = denominations.length - 1; i >= 0; i--) {
            Denomination d = denominations[i];
            int value = d.getValue();
            int available = temp.getOrDefault(d, 0);
            int needed = remaining / value;
            int toUse = Math.min(needed, available);

            if (toUse > 0) {
                change.put(d, toUse);
                temp.put(d, available - toUse);
                remaining -= toUse * value;
            }

            if (remaining == 0) {
                // Commit the changes
                inventory.putAll(temp);
                return change;
            }
        }

        // Cannot give exact change
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CashInventory{");
        for (Denomination d : Denomination.values()) {
            sb.append(d).append("=").append(inventory.get(d)).append(", ");
        }
        sb.append("total=").append(getTotalValue()).append("}");
        return sb.toString();
    }
}
