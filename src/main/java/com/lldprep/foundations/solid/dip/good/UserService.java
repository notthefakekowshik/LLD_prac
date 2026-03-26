// DIP GOOD: High-level module depends ONLY on the UserRepository abstraction.
// The concrete implementation is injected via constructor — caller decides which one to use.
// Swapping MySQL for InMemory (or any future DB) requires zero changes here.
package com.lldprep.foundations.solid.dip.good;

public class UserService {

    // Depends on abstraction, not a concrete class
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public void registerUser(String username) {
        System.out.println("[DIP-Good] Registering user: " + username);
        repository.save(username);
        String found = repository.findByUsername(username);
        System.out.println("[DIP-Good] Confirmed: " + found);
    }
}
