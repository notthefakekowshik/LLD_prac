package com.lldprep.systems.meetingroomscheduler.repository;

import com.lldprep.systems.meetingroomscheduler.model.Room;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RoomRepository {
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    public void save(Room room) {
        rooms.put(room.getId(), room);
    }

    public Room getById(String roomId) {
        return rooms.get(roomId);
    }

    public List<Room> getAll() {
        return new ArrayList<>(rooms.values());
    }

    public int count() {
        return rooms.size();
    }
}
