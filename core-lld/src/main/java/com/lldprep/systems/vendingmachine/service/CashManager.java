package com.lldprep.systems.vendingmachine.service;

import com.lldprep.systems.vendingmachine.model.CashInventory;
import com.lldprep.systems.vendingmachine.model.enums.Denomination;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Manages cash operations for the vending machine.
 * Handles accepting payments and returning change.
 */
public class CashManager {
    private final CashInventory cashInventory;
    private BigDecimal currentSessionAmount = BigDecimal.ZERO;

    public CashManager(CashInventory cashInventory) {
        this.cashInventory = cashInventory;
    }

    /**
     * Accept a denomination during a session.
     * Adds to inventory and tracks session amount.
     */
    public void acceptMoney(Denomination denomination) {
        cashInventory.addCash(denomination, 1);
        currentSessionAmount = currentSessionAmount.add(BigDecimal.valueOf(denomination.getValue()));
    }

    /**
     * Get amount inserted in current session.
     */
    public BigDecimal getSessionAmount() {
        return currentSessionAmount;
    }

    /**
     * Reset session amount (called on cancel or complete).
     */
    public void resetSession() {
        // Refund the session amount from inventory
        if (currentSessionAmount.compareTo(BigDecimal.ZERO) > 0) {
            refundSessionAmount();
        }
        currentSessionAmount = BigDecimal.ZERO;
    }

    /**
     * Check if we can give change for the difference.
     */
    public boolean canGiveChange(BigDecimal price) {
        BigDecimal change = currentSessionAmount.subtract(price);
        if (change.compareTo(BigDecimal.ZERO) <= 0) {
            return true; // No change needed
        }
        return cashInventory.canGiveChange(change);
    }

    /**
     * Calculate and dispense change.
     * @return Map of denomination to count, or null if cannot give change
     */
    public Map<Denomination, Integer> dispenseChange(BigDecimal price) {
        BigDecimal change = currentSessionAmount.subtract(price);
        if (change.compareTo(BigDecimal.ZERO) <= 0) {
            return null; // No change needed
        }
        return cashInventory.calculateChange(change);
    }

    /**
     * Finalize transaction - session amount stays in machine, change given out.
     */
    public void finalizeTransaction() {
        currentSessionAmount = BigDecimal.ZERO;
    }

    /**
     * Check if machine requires exact change (low on small denominations).
     */
    public boolean requiresExactChange() {
        // If we have less than 5 of each coin denomination, require exact change
        int coin1 = cashInventory.getCount(Denomination.COIN_1);
        int coin5 = cashInventory.getCount(Denomination.COIN_5);
        int coin10 = cashInventory.getCount(Denomination.COIN_10);

        return coin1 < 5 || coin5 < 5 || coin10 < 5;
    }

    /**
     * Get change inventory status.
     */
    public CashInventory getCashInventory() {
        return cashInventory;
    }

    private void refundSessionAmount() {
        // Remove the session amount from inventory (return to user)
        // This is a simplified refund - in reality, we'd track exactly which notes were inserted
        BigDecimal toRefund = currentSessionAmount;
        // Try to refund using available denominations
        for (Denomination d : Denomination.values()) {
            int value = d.getValue();
            while (toRefund.intValue() >= value && cashInventory.hasDenomination(d, 1)) {
                cashInventory.removeCash(d, 1);
                toRefund = toRefund.subtract(BigDecimal.valueOf(value));
                if (toRefund.intValue() <= 0) break;
            }
            if (toRefund.intValue() <= 0) break;
        }
    }
}
