package com.lldprep.systems.parkinglot.model;

public class Truck extends Vehicle {
    public Truck(String vehicleId, String licensePlate) {
        super(vehicleId, licensePlate, VehicleType.TRUCK);
    }
}
