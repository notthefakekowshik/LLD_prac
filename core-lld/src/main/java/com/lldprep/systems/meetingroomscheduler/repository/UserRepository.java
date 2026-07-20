package com.lldprep.systems.meetingroomscheduler.repository;

import com.lldprep.systems.meetingroomscheduler.model.User;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserRepository {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, String> emailToUserId = new ConcurrentHashMap<>();

    // Atomic register-if-new: putIfAbsent on the email index is the guard, so two
    // threads racing the same email can't both create a user.
    public boolean saveIfEmailAbsent(User user) {
        String existing = emailToUserId.putIfAbsent(user.getEmail().toLowerCase(), user.getId());
        if (existing != null) return false;
        users.put(user.getId(), user);
        return true;
    }

    public User getById(String userId) {
        return users.get(userId);
    }

    public int count() {
        return users.size();
    }
}
