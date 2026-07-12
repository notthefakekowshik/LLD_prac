package com.lldprep.systems.meetingroomscheduler.exception;

public class UserNotFoundException extends MeetingSchedulerException {
    public UserNotFoundException(String userId) {
        super("User not found: " + userId);
    }
}
