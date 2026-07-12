package com.lldprep.systems.meetingroomscheduler.exception;

public class DuplicateEmailException extends MeetingSchedulerException {
    public DuplicateEmailException(String email) {
        super("A user with email " + email + " already exists");
    }
}
