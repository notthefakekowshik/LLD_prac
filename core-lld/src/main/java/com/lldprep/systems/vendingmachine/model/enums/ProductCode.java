package com.lldprep.systems.vendingmachine.model.enums;

/**
 * Slot codes for the vending machine grid.
 * 3 rows (A, B, C) x 5 columns (1-5) = 15 slots
 */
public enum ProductCode {
    A1("A1"), A2("A2"), A3("A3"), A4("A4"), A5("A5"),
    B1("B1"), B2("B2"), B3("B3"), B4("B4"), B5("B5"),
    C1("C1"), C2("C2"), C3("C3"), C4("C4"), C5("C5");

    private final String code;

    ProductCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * Parse a string code to ProductCode enum.
     * @throws IllegalArgumentException if code is invalid
     */
    public static ProductCode fromString(String code) {
        for (ProductCode pc : values()) {
            if (pc.code.equalsIgnoreCase(code)) {
                return pc;
            }
        }
        throw new IllegalArgumentException("Invalid product code: " + code);
    }

    @Override
    public String toString() {
        return code;
    }
}
