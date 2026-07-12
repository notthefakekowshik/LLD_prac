package com.lldprep.systems.meetingroomscheduler.exception;

public class RoomNotFoundException extends MeetingSchedulerException {
    public RoomNotFoundException(String roomId) {
        super("Room not found: " + roomId);
    }
}
