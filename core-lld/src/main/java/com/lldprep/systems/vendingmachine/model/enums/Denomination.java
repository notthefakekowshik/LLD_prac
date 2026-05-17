package com.lldprep.systems.vendingmachine.model.enums;

/**
 * Denominations accepted by the vending machine.
 * Coins: 1, 5, 10 rupees
 * Notes: 20, 50, 100 rupees
 */
public enum Denomination {
    COIN_1(1, "Coin", "₹1"),
    COIN_5(5, "Coin", "₹5"),
    COIN_10(10, "Coin", "₹10"),
    NOTE_20(20, "Note", "₹20"),
    NOTE_50(50, "Note", "₹50"),
    NOTE_100(100, "Note", "₹100");

    private final int value;
    private final String type;
    private final String display;

    Denomination(int value, String type, String display) {
        this.value = value;
        this.type = type;
        this.display = display;
    }

    public int getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    public String getDisplay() {
        return display;
    }

    @Override
    public String toString() {
        return display;
    }
}
