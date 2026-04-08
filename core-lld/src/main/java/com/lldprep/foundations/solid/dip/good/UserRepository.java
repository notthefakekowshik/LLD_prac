// DIP GOOD: Abstraction that both high-level (UserService) and low-level modules depend on.
// High-level module owns this interface — it defines what it NEEDS, not how it is done.
// Low-level modules (MySQL, InMemory) implement this contract.
package com.lldprep.foundations.solid.dip.good;

public interface UserRepository {
    void save(String username);
    String findByUsername(String username);
}
