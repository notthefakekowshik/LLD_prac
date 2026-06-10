package com.lldprep.systems.splitwise.repository;

import com.lldprep.systems.splitwise.model.Group;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GroupRepository {
    private final Map<String, Group> groups = new ConcurrentHashMap<>();

    public void save(Group group) {
        groups.put(group.getId(), group);
    }

    public Group getById(String groupId) {
        return groups.get(groupId);
    }

    public List<Group> getAll() {
        return new ArrayList<>(groups.values());
    }

    public int count() {
        return groups.size();
    }
}
