package com.lldprep.systems.splitwise.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Group {
    private final String id;
    private final String name;
    private final List<User> members;
    private final LocalDateTime createdAt;

    public Group(String name, List<User> members) {
        this.id = "GRP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.name = name;
        this.members = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        for (User member : members) {
            addMember(member);
        }
    }

    public synchronized void addMember(User user) {
        if (!hasMember(user.getId())) {
            members.add(user);
        }
    }

    public synchronized void removeMember(String userId) {
        members.removeIf(member -> member.getId().equals(userId));
    }

    public synchronized boolean hasMember(String userId) {
        return members.stream().anyMatch(member -> member.getId().equals(userId));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public synchronized List<User> getMembers() {
        return new ArrayList<>(members);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public synchronized String toString() {
        return name + " (" + id + ", members=" + members.size() + ")";
    }
}
