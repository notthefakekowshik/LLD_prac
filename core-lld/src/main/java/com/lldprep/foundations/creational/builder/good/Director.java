package com.lldprep.foundations.creational.builder.good;

/**
 * Director - orchestrates common build sequences.
 * 
 * USE CASE: When same construction steps are repeated.
 * Director knows WHAT to build, Builder knows HOW.
 */
public class Director {
    
    /**
     * Build a gaming computer.
     */
    public Computer buildGamingPC(Computer.Builder builder) {
        return builder
            .storage("2TB NVMe SSD")
            .gpu("RTX 4090")
            .powerSupply("1000W")
            .liquidCooling(true)
            .build();
    }
    
    /**
     * Build an office computer.
     */
    public Computer buildOfficePC(Computer.Builder builder) {
        return builder
            .storage("256GB SSD")
            .gpu("Integrated")
            .powerSupply("450W")
            .liquidCooling(false)
            .build();
    }
    
    /**
     * Build a margherita pizza.
     */
    public Pizza buildMargherita(Pizza.Builder<?> builder) {
        return builder
            .name("Margherita")
            .sauce("Tomato")
            .addTopping("Mozzarella")
            .addTopping("Basil")
            .build();
    }
    
    /**
     * Build a pepperoni pizza.
     */
    public Pizza buildPepperoni(Pizza.Builder<?> builder) {
        return builder
            .name("Pepperoni")
            .sauce("Tomato")
            .dough("Hand-tossed")
            .addTopping("Mozzarella")
            .addTopping("Pepperoni")
            .addTopping("Oregano")
            .build();
    }
}
