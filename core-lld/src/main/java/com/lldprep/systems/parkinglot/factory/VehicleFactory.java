package com.lldprep.systems.parkinglot.factory;

import com.lldprep.systems.parkinglot.model.*;

import java.util.UUID;

// Why: centralises vehicle creation so callers never depend on concrete Bike/Car/Truck
//      constructors — adding an ElectricCar subtype is a zero-edit change to existing code (OCP).
public class VehicleFactory {

    public static Vehicle create(VehicleType type, String licensePlate) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        return switch (type) {
            case BIKE  -> new Bike(id, licensePlate);
            case CAR   -> new Car(id, licensePlate);
            case TRUCK -> new Truck(id, licensePlate);
        };
    }
}
