package com.lldprep.systems.parkinglot.model;

public class Bike extends Vehicle {
    public Bike(String vehicleId, String licensePlate) {
        super(vehicleId, licensePlate, VehicleType.BIKE);
    }
}
