package com.lldprep.systems.meetingroomscheduler.policy;

import com.lldprep.systems.meetingroomscheduler.model.Room;

import java.util.List;

public class AmenityFilter implements RoomFilter {
    @Override
    public List<Room> filter(List<Room> rooms, SearchCriteria criteria) {
        if (criteria.requiredAmenities().isEmpty()) {
            return rooms;
        }
        return rooms.stream()
            .filter(room -> room.getAmenities().containsAll(criteria.requiredAmenities()))
            .toList();
    }
}
