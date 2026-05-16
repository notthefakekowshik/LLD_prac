package com.lldprep.systems.parkinglot.exception;

public class InvalidTicketException extends ParkingLotException {
    public InvalidTicketException(String ticketId) {
        super("Invalid or already checked-out ticket: " + ticketId);
    }
}
