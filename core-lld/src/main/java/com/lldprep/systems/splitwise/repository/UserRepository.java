package com.lldprep.systems.splitwise.repository;

import com.lldprep.systems.splitwise.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class UserRepository {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, String> emailToUserId = new ConcurrentHashMap<>();

    public void save(User user) {
        users.put(user.getId(), user);
        emailToUserId.put(user.getEmail().toLowerCase(), user.getId());
    }

    public boolean saveIfEmailAbsent(User user) {
        String existing = emailToUserId.putIfAbsent(user.getEmail().toLowerCase(), user.getId());
        if (existing != null) return false;
        users.put(user.getId(), user);
        return true;
    }

    public User getById(String userId) {
        return users.get(userId);
    }

    public Optional<User> findByEmail(String email) {
        String userId = emailToUserId.get(email.toLowerCase());
        return userId == null ? Optional.empty() : Optional.ofNullable(users.get(userId));
    }

    public List<User> getAll() {
        return new ArrayList<>(users.values());
    }

    public int count() {
        return users.size();
    }
}
