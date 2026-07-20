package com.lldprep.systems.meetingroomscheduler.model;

import java.time.LocalDate;
import java.util.BitSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Room {
    private final String id;
    private final String name;
    private final int capacity;
    private final int floor;
    private final Set<Amenity> amenities;
    private final Map<LocalDate, BitSet> schedule;

    private static final int TOTAL_SLOTS = 18;

    public Room(String name, int capacity, int floor, Set<Amenity> amenities) {
        this.id = "RM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.name = name;
        this.capacity = capacity;
        this.floor = floor;
        this.amenities = Set.copyOf(amenities);
        this.schedule = new ConcurrentHashMap<>();
    }

    public synchronized boolean isSlotAvailable(TimeSlot slot) {
        BitSet dayBits = schedule.get(slot.date());
        if (dayBits == null) {
            return true;
        }
        int firstSet = dayBits.nextSetBit(slot.startSlot());
        return firstSet == -1 || firstSet >= slot.endSlot();
    }

    public synchronized boolean bookSlot(TimeSlot slot) {
        BitSet dayBits = schedule.computeIfAbsent(slot.date(), d -> new BitSet(TOTAL_SLOTS));
        int firstSet = dayBits.nextSetBit(slot.startSlot());
        if (firstSet != -1 && firstSet < slot.endSlot()) {
            return false;
        }
        dayBits.set(slot.startSlot(), slot.endSlot());
        return true;
    }

    public synchronized void freeSlot(TimeSlot slot) {
        BitSet dayBits = schedule.get(slot.date());
        if (dayBits != null) {
            dayBits.clear(slot.startSlot(), slot.endSlot());
        }
    }

    public synchronized String getScheduleForDate(LocalDate date) {
        BitSet dayBits = schedule.get(date);
        if (dayBits == null) {
            return "All slots available on " + date;
        }
        StringBuilder sb = new StringBuilder(date + " schedule:\n");
        for (int slot = 0; slot < TOTAL_SLOTS; slot++) {
            int hour = 9 + slot / 2;
            int minute = (slot % 2) * 30;
            sb.append(String.format("  %02d:%02d  [%s]\n",
                hour, minute, dayBits.get(slot) ? "BOOKED" : "FREE"));
        }
        return sb.toString();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getFloor() {
        return floor;
    }

    public Set<Amenity> getAmenities() {
        return amenities;
    }

    @Override
    public String toString() {
        return name + " (floor " + floor + ", cap " + capacity + ", amenities=" + amenities + ")";
    }
}
