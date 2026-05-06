package com.lldprep.systems.atm.dispenser;

import com.lldprep.systems.atm.model.CashInventory;
import com.lldprep.systems.atm.model.enums.Denomination;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

public abstract class CashDispenser {
    protected final Denomination denomination;
    protected CashDispenser nextDispenser;

    protected CashDispenser(Denomination denomination) {
        this.denomination = denomination;
    }

    public void setNext(CashDispenser next) {
        this.nextDispenser = next;
    }

    public Map<Denomination, Integer> dispense(BigDecimal amount, CashInventory inventory) {
        Map<Denomination, Integer> result = new EnumMap<>(Denomination.class);
        
        if (canHandle(amount)) {
            BigDecimal remaining = amount;
            int noteValue = denomination.getValue();
            
            // Calculate how many notes of this denomination we can use
            int availableNotes = inventory.getAvailableNotes(denomination);
            int neededNotes = remaining.divide(BigDecimal.valueOf(noteValue)).intValue();
            int notesToDispense = Math.min(neededNotes, availableNotes);
            
            if (notesToDispense > 0) {
                BigDecimal dispensedAmount = BigDecimal.valueOf(notesToDispense * noteValue);
                remaining = remaining.subtract(dispensedAmount);
                result.put(denomination, notesToDispense);
            }
            
            // If still remaining, pass to next handler
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                if (nextDispenser != null) {
                    Map<Denomination, Integer> nextResult = nextDispenser.dispense(remaining, inventory);
                    if (nextResult == null) {
                        return null; // Cannot fulfill with available notes
                    }
                    result.putAll(nextResult);
                } else {
                    return null; // No more handlers and still have remaining amount
                }
            }
            
            return result;
        } else if (nextDispenser != null) {
            return nextDispenser.dispense(amount, inventory);
        }
        
        return null;
    }

    public boolean canHandle(BigDecimal amount) {
        return amount.compareTo(BigDecimal.valueOf(denomination.getValue())) >= 0;
    }

    protected Map<Denomination, Integer> dispenseDenomination(BigDecimal amount, int noteValue, 
                                                               CashInventory inventory, int availableNotes) {
        Map<Denomination, Integer> result = new EnumMap<>(Denomination.class);
        int neededNotes = amount.divide(BigDecimal.valueOf(noteValue)).intValue();
        int notesToDispense = Math.min(neededNotes, availableNotes);
        
        if (notesToDispense > 0) {
            result.put(denomination, notesToDispense);
        }
        
        return result;
    }
}
