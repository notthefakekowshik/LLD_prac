// DIP setup (concrete low-level module): A MySQL-specific repository implementation.
// The problem isn't this class — it's that UserService instantiates it directly.
package com.lldprep.foundations.solid.dip.bad;

public class MySQLUserRepository {

    public void save(String username) {
        System.out.println("[MySQL] Saving user: " + username);
    }

    public String findByUsername(String username) {
        // Simulated lookup
        System.out.println("[MySQL] Finding user: " + username);
        return "User{name=" + username + ", db=MySQL}";
    }
}
