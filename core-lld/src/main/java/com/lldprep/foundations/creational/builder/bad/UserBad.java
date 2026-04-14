package com.lldprep.foundations.creational.builder.bad;

/**
 * BAD: Constructor telescoping for object creation.
 * 
 * PROBLEMS:
 * 1. Too many constructors - "telescoping constructor" anti-pattern
 * 2. Parameter confusion - which boolean is what?
 * 3. Immutable objects impossible (need setters)
 * 4. Invalid object states possible
 * 5. Code unreadable at call site
 */
public class UserBad {
    private String firstName;
    private String lastName;
    private int age;
    private String phone;
    private String address;
    private boolean emailNotifications;
    private boolean smsNotifications;
    private boolean marketingEmails;
    
    // TELESCOPING CONSTRUCTORS - NIGHTMARE!
    public UserBad(String firstName, String lastName) {
        this(firstName, lastName, 0);
    }
    
    public UserBad(String firstName, String lastName, int age) {
        this(firstName, lastName, age, "");
    }
    
    public UserBad(String firstName, String lastName, int age, String phone) {
        this(firstName, lastName, age, phone, "");
    }
    
    public UserBad(String firstName, String lastName, int age, String phone, String address) {
        this(firstName, lastName, age, phone, address, false, false, false);
    }
    
    // THE MONSTER - 8 parameters, hard to read at call site
    public UserBad(String firstName, String lastName, int age, String phone, 
                   String address, boolean emailNotifications, 
                   boolean smsNotifications, boolean marketingEmails) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
        this.phone = phone;
        this.address = address;
        this.emailNotifications = emailNotifications;
        this.smsNotifications = smsNotifications;
        this.marketingEmails = marketingEmails;
    }
    
    // Client call is unreadable:
    // new UserBad("John", "Doe", 30, "123-456", "NYC", true, false, true);
    // Which boolean is which? No idea without IDE!
}
