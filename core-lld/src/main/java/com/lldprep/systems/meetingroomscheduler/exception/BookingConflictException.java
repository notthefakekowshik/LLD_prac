package com.lldprep.systems.meetingroomscheduler.exception;

public class BookingConflictException extends MeetingSchedulerException {
    public BookingConflictException(String roomName) {
        super("Room " + roomName + " is already booked for the requested time slot");
    }
}
