package com.lldprep.systems.parkinglot.model;

import java.time.LocalDateTime;

public class Ticket {
    private final String ticketId;
    private final Vehicle vehicle;
    private final ParkingSpot spot;
    private final LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private double fee;

    public Ticket(String ticketId, Vehicle vehicle, ParkingSpot spot, LocalDateTime entryTime) {
        this.ticketId = ticketId;
        this.vehicle = vehicle;
        this.spot = spot;
        this.entryTime = entryTime;
    }

    public void checkout(LocalDateTime exitTime, double fee) {
        this.exitTime = exitTime;
        this.fee = fee;
    }

    public String getTicketId()       { return ticketId; }
    public Vehicle getVehicle()       { return vehicle; }
    public ParkingSpot getSpot()      { return spot; }
    public LocalDateTime getEntryTime() { return entryTime; }
    public LocalDateTime getExitTime()  { return exitTime; }
    public double getFee()            { return fee; }
    public boolean isCheckedOut()     { return exitTime != null; }

    @Override
    public String toString() {
        return "Ticket[" + ticketId + " | " + vehicle + " | " + spot + " | in=" + entryTime + "]";
    }
}
