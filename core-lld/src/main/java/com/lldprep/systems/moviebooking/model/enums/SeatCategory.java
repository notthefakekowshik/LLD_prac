package com.lldprep.systems.moviebooking.model.enums;

public enum SeatCategory {
    REGULAR("Regular", 1.0),
    PREMIUM("Premium", 1.4),
    RECLINER("Recliner", 2.0);

    private final String label;
    private final double priceMultiplier;

    SeatCategory(String label, double priceMultiplier) {
        this.label = label;
        this.priceMultiplier = priceMultiplier;
    }

    public double getPriceMultiplier() {
        return priceMultiplier;
    }

    @Override
    public String toString() {
        return label;
    }
}
