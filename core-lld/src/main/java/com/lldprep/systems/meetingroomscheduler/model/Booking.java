package com.lldprep.systems.meetingroomscheduler.model;

import java.util.UUID;

public class Booking {
    private final String id;
    private final Room room;
    private final User user;
    private final TimeSlot timeSlot;

    public Booking(Room room, User user, TimeSlot timeSlot) {
        this.id = "BKG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.room = room;
        this.user = user;
        this.timeSlot = timeSlot;
    }

    public String getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public User getUser() {
        return user;
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    @Override
    public String toString() {
        return "Booking[" + id + "] " + room.getName() + " | " + timeSlot.date()
            + " | " + timeSlot.display() + " | by " + user.getName();
    }
}
