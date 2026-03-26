// DIP VIOLATION: Hard-wired to MySQL. Cannot swap implementation.
// High-level module (UserService) directly depends on a low-level module (MySQLUserRepository).
// To switch to InMemory or PostgreSQL, you must edit this class — violating DIP and OCP.
package com.lldprep.foundations.solid.dip.bad;

public class UserService {

    // DIP VIOLATION: Concrete dependency — UserService is married to MySQL.
    private final MySQLUserRepository repository;

    public UserService() {
        // Hard-coded instantiation: impossible to inject a different implementation
        this.repository = new MySQLUserRepository();
    }

    public void registerUser(String username) {
        System.out.println("[BAD-DIP] Registering user: " + username);
        repository.save(username);
        String found = repository.findByUsername(username);
        System.out.println("[BAD-DIP] Confirmed: " + found);
    }
}
