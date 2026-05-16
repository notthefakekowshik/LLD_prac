package com.lldprep.systems.parkinglot.model;

public class ParkingSpot {
    private final String spotId;
    private final int levelNumber;
    private final int spotNumber;
    private final SpotType spotType;
    private SpotStatus status;
    private Vehicle parkedVehicle;

    public ParkingSpot(String spotId, int levelNumber, int spotNumber, SpotType spotType) {
        this.spotId = spotId;
        this.levelNumber = levelNumber;
        this.spotNumber = spotNumber;
        this.spotType = spotType;
        this.status = SpotStatus.AVAILABLE;
    }

    // synchronized on the spot — guards status + parkedVehicle as an atomic unit
    public synchronized boolean park(Vehicle vehicle) {
        if (status != SpotStatus.AVAILABLE) return false;
        this.parkedVehicle = vehicle;
        this.status = SpotStatus.OCCUPIED;
        return true;
    }

    public synchronized void vacate() {
        this.parkedVehicle = null;
        this.status = SpotStatus.AVAILABLE;
    }

    public synchronized boolean isAvailable() {
        return status == SpotStatus.AVAILABLE;
    }

    public String getSpotId()            { return spotId; }
    public int getLevelNumber()          { return levelNumber; }
    public int getSpotNumber()           { return spotNumber; }
    public SpotType getSpotType()        { return spotType; }
    public synchronized SpotStatus getStatus()         { return status; }
    public synchronized Vehicle getParkedVehicle()     { return parkedVehicle; }

    @Override
    public String toString() {
        return "Spot[L" + levelNumber + "-#" + spotNumber + "(" + spotType + ")=" + status + "]";
    }
}
