package com.lldprep.systems.parkinglot.model;

public enum SpotType {
    COMPACT,  // bikes only
    REGULAR,  // cars and bikes
    LARGE;    // trucks, cars, and bikes

    public boolean canFit(VehicleType vehicleType) {
        return switch (vehicleType) {
            case BIKE  -> true;
            case CAR   -> this == REGULAR || this == LARGE;
            case TRUCK -> this == LARGE;
        };
    }

    public static SpotType preferredFor(VehicleType vehicleType) {
        return switch (vehicleType) {
            case BIKE  -> COMPACT;
            case CAR   -> REGULAR;
            case TRUCK -> LARGE;
        };
    }
}
