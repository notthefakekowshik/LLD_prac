package com.lldprep.systems.meetingroomscheduler.policy;

import com.lldprep.systems.meetingroomscheduler.model.Room;

import java.util.List;

public interface RoomFilter {
    List<Room> filter(List<Room> rooms, SearchCriteria criteria);
}
