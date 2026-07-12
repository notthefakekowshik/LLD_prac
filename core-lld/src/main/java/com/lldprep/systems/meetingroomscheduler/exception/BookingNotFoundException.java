package com.lldprep.systems.meetingroomscheduler.exception;

public class BookingNotFoundException extends MeetingSchedulerException {
    public BookingNotFoundException(String bookingId) {
        super("Booking not found: " + bookingId);
    }
}
