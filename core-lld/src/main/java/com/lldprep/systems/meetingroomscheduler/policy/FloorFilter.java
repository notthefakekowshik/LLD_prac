package com.lldprep.systems.meetingroomscheduler.policy;

import com.lldprep.systems.meetingroomscheduler.model.Room;

import java.util.List;

public class FloorFilter implements RoomFilter {
    @Override
    public List<Room> filter(List<Room> rooms, SearchCriteria criteria) {
        if (!criteria.floorSpecified()) {
            return rooms;
        }
        int floor = criteria.floor();
        return rooms.stream()
            .filter(room -> room.getFloor() == floor)
            .toList();
    }
}
