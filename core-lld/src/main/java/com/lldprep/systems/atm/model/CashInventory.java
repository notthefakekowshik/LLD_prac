package com.lldprep.systems.atm.model;

import com.lldprep.systems.atm.model.enums.Denomination;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class CashInventory {
    private final Map<Denomination, Integer> notes;

    public CashInventory() {
        this.notes = new EnumMap<>(Denomination.class);
        for (Denomination d : Denomination.values()) {
            notes.put(d, 0);
        }
    }

    public synchronized void addCash(Denomination denomination, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count cannot be negative");
        }
        notes.put(denomination, notes.get(denomination) + count);
    }

    public synchronized void addCash(Map<Denomination, Integer> cash) {
        for (Map.Entry<Denomination, Integer> entry : cash.entrySet()) {
            addCash(entry.getKey(), entry.getValue());
        }
    }

    public synchronized void dispense(Denomination denomination, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count cannot be negative");
        }
        int available = notes.get(denomination);
        if (available < count) {
            throw new IllegalStateException(
                String.format("Insufficient %s notes. Available: %d, Requested: %d",
                    denomination, available, count));
        }
        notes.put(denomination, available - count);
    }

    public synchronized void dispense(Map<Denomination, Integer> cash) {
        for (Map.Entry<Denomination, Integer> entry : cash.entrySet()) {
            dispense(entry.getKey(), entry.getValue());
        }
    }

    public synchronized boolean hasSufficientCash(BigDecimal amount) {
        return getTotalCash().compareTo(amount) >= 0;
    }

    public synchronized int getAvailableNotes(Denomination denomination) {
        return notes.getOrDefault(denomination, 0);
    }

    public synchronized BigDecimal getTotalCash() {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Denomination, Integer> entry : notes.entrySet()) {
            BigDecimal value = BigDecimal.valueOf(entry.getKey().getValue());
            total = total.add(value.multiply(BigDecimal.valueOf(entry.getValue())));
        }
        return total;
    }

    public synchronized Map<Denomination, Integer> getNotesSnapshot() {
        return new EnumMap<>(notes);
    }

    @Override
    public synchronized String toString() {
        StringBuilder sb = new StringBuilder("CashInventory{");
        for (Denomination d : Denomination.values()) {
            sb.append(String.format("%s=%d, ", d, notes.get(d)));
        }
        sb.append("total=").append(getTotalCash()).append("}");
        return sb.toString();
    }
}
