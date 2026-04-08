// DIP GOOD: Alternative low-level module — plug-in replacement for MySQL.
// UserService never knows this exists; it just uses the UserRepository abstraction.
// Useful for tests, local development, or any scenario where MySQL is unavailable.
package com.lldprep.foundations.solid.dip.good;

import java.util.HashMap;
import java.util.Map;

public class InMemoryUserRepository implements UserRepository {

    private final Map<String, String> store = new HashMap<>();

    @Override
    public void save(String username) {
        store.put(username, "User{name=" + username + ", db=InMemory}");
        System.out.println("[InMemory] Saved user: " + username);
    }

    @Override
    public String findByUsername(String username) {
        System.out.println("[InMemory] Finding user: " + username);
        return store.getOrDefault(username, "User{name=" + username + ", db=InMemory}");
    }
}
