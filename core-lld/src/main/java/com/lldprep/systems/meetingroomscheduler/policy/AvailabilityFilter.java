package com.lldprep.systems.meetingroomscheduler.policy;

import com.lldprep.systems.meetingroomscheduler.model.Room;

import java.util.List;

public class AvailabilityFilter implements RoomFilter {
    @Override
    public List<Room> filter(List<Room> rooms, SearchCriteria criteria) {
        return rooms.stream()
            .filter(room -> room.isSlotAvailable(criteria.timeSlot()))
            .toList();
    }
}
