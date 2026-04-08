// DIP GOOD: Low-level module depends on the abstraction (UserRepository), not the other way around.
// Can be swapped for any other UserRepository implementation without touching UserService.
package com.lldprep.foundations.solid.dip.good;

public class MySQLUserRepository implements UserRepository {

    @Override
    public void save(String username) {
        System.out.println("[MySQL] Saving user: " + username);
    }

    @Override
    public String findByUsername(String username) {
        System.out.println("[MySQL] Finding user: " + username);
        return "User{name=" + username + ", db=MySQL}";
    }
}
