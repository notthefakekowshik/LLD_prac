package com.lldprep.foundations.creational.builder.good;

/**
 * Product class built by Builder.
 * Immutable - all fields final, no setters.
 */
public class User {
    private final String firstName;
    private final String lastName;
    private final int age;
    private final String phone;
    private final String address;
    private final boolean emailNotifications;
    private final boolean smsNotifications;
    private final boolean marketingEmails;
    
    // Private constructor - only Builder can create
    private User(Builder builder) {
        this.firstName = builder.firstName;
        this.lastName = builder.lastName;
        this.age = builder.age;
        this.phone = builder.phone;
        this.address = builder.address;
        this.emailNotifications = builder.emailNotifications;
        this.smsNotifications = builder.smsNotifications;
        this.marketingEmails = builder.marketingEmails;
    }
    
    // Getters only - immutable
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public int getAge() { return age; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public boolean isEmailNotifications() { return emailNotifications; }
    public boolean isSmsNotifications() { return smsNotifications; }
    public boolean isMarketingEmails() { return marketingEmails; }
    
    @Override
    public String toString() {
        return "User{" +
            "firstName='" + firstName + '\'' +
            ", lastName='" + lastName + '\'' +
            ", age=" + age +
            ", phone='" + phone + '\'' +
            ", address='" + address + '\'' +
            ", emailNotifications=" + emailNotifications +
            ", smsNotifications=" + smsNotifications +
            ", marketingEmails=" + marketingEmails +
            '}';
    }
    
    /**
     * Builder - static inner class for fluent construction.
     */
    public static class Builder {
        // Required parameters
        private final String firstName;
        private final String lastName;
        
        // Optional parameters with defaults
        private int age = 0;
        private String phone = "";
        private String address = "";
        private boolean emailNotifications = false;
        private boolean smsNotifications = false;
        private boolean marketingEmails = false;
        
        public Builder(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }
        
        public Builder age(int age) {
            this.age = age;
            return this;
        }
        
        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }
        
        public Builder address(String address) {
            this.address = address;
            return this;
        }
        
        public Builder emailNotifications(boolean enabled) {
            this.emailNotifications = enabled;
            return this;
        }
        
        public Builder smsNotifications(boolean enabled) {
            this.smsNotifications = enabled;
            return this;
        }
        
        public Builder marketingEmails(boolean enabled) {
            this.marketingEmails = enabled;
            return this;
        }
        
        public User build() {
            return new User(this);
        }
    }
}
