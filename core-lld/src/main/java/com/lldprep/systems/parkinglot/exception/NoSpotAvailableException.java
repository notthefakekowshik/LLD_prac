package com.lldprep.systems.parkinglot.exception;

import com.lldprep.systems.parkinglot.model.VehicleType;

public class NoSpotAvailableException extends ParkingLotException {
    public NoSpotAvailableException(VehicleType vehicleType) {
        super("No available spot for vehicle type: " + vehicleType);
    }
}
