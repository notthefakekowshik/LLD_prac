package com.lldprep.systems.meetingroomscheduler.policy;

import com.lldprep.systems.meetingroomscheduler.model.Room;

import java.util.List;

public class CapacityFilter implements RoomFilter {
    @Override
    public List<Room> filter(List<Room> rooms, SearchCriteria criteria) {
        int minCapacity = criteria.minCapacity();
        return rooms.stream()
            .filter(room -> room.getCapacity() >= minCapacity)
            .toList();
    }
}
