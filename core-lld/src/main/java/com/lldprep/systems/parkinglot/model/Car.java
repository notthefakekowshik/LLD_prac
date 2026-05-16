package com.lldprep.systems.parkinglot.model;

public class Car extends Vehicle {
    public Car(String vehicleId, String licensePlate) {
        super(vehicleId, licensePlate, VehicleType.CAR);
    }
}
