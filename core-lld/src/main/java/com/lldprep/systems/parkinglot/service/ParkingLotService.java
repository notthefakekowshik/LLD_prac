package com.lldprep.systems.parkinglot.service;

import com.lldprep.systems.parkinglot.model.ParkingLotMetrics;
import com.lldprep.systems.parkinglot.model.Ticket;
import com.lldprep.systems.parkinglot.model.Vehicle;
import com.lldprep.systems.parkinglot.model.VehicleType;

public interface ParkingLotService {

    /** Parks the vehicle, marks spot OCCUPIED, and returns a ticket.
     *  Throws NoSpotAvailableException if no compatible spot is free. */
    Ticket checkIn(Vehicle vehicle);

    /** Vacates the spot, computes fee, and returns the amount due.
     *  Throws InvalidTicketException for unknown or already-checked-out tickets. */
    double checkOut(String ticketId);

    /** Count of spots (any type) that can accept the given vehicle. */
    long getAvailableSpots(VehicleType vehicleType);

    /** Snapshot of current occupancy and revenue metrics. */
    ParkingLotMetrics getMetrics();
}
