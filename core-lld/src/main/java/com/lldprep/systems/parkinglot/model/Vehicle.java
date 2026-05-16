package com.lldprep.systems.parkinglot.model;

public abstract class Vehicle {
    private final String vehicleId;
    private final String licensePlate;
    private final VehicleType vehicleType;

    protected Vehicle(String vehicleId, String licensePlate, VehicleType vehicleType) {
        this.vehicleId = vehicleId;
        this.licensePlate = licensePlate;
        this.vehicleType = vehicleType;
    }

    public String getVehicleId()      { return vehicleId; }
    public String getLicensePlate()   { return licensePlate; }
    public VehicleType getVehicleType() { return vehicleType; }

    @Override
    public String toString() {
        return vehicleType + "[" + licensePlate + "]";
    }
}
