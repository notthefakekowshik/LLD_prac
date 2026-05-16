package com.lldprep.systems.parkinglot.model;

public record ParkingLotMetrics(
    long totalSpots,
    long availableSpots,
    long occupiedSpots,
    long ticketsIssued,
    long checkoutsProcessed,
    double totalRevenue
) {
    public double occupancyRate() {
        return totalSpots == 0 ? 0.0 : (double) occupiedSpots / totalSpots * 100;
    }
}
