package com.lldprep.foundations.creational.builder.good;

/**
 * BUILDER PATTERN
 * ===============
 *
 * WHY IT EXISTS:
 * Constructs complex objects step by step. Allows creating different
 * representations of the same object using the same construction code.
 *
 * PROBLEMS IT SOLVES:
 * - Telescoping constructors (5, 10, 20+ parameter constructors)
 * - Parameter confusion at call site (which boolean is which?)
 * - Invalid intermediate object states during construction
 * - Cannot make objects immutable when using setters
 * - Complex object construction mixed with business logic
 *
 * WHEN TO USE:
 * - Objects with many optional parameters (User, Query, Request, Config)
 * - Step-by-step construction needed (must validate before each step)
 * - Different representations of same object (HTML vs JSON document)
 * - Immutable objects with many fields (no setters after creation)
 * - SQL queries, HTTP requests, Configuration objects
 *
 * KEY BENEFITS:
 * - Readable: Method names document what each parameter means
 * - Flexible: Can add optional fields in any order
 * - Immutable: Object is fully constructed before use
 * - Validated: Can enforce business rules before building
 *
 * VARIATIONS SHOWN:
 * - Basic Builder: Step-by-step with fluent API
 * - Validation Builder: Business rules enforced at build()
 * - Hierarchical Builder: Builder inheritance for class hierarchies
 * - Director: Predefined construction sequences for common configurations
 *
 * REAL-WORLD EXAMPLES:
 * - Java StringBuilder
 * - Lombok @Builder annotation
 * - Retrofit/OkHttp Request builders
 * - JPA Criteria API
 * - QueryDSL
 *
 * @see <a href="https://en.wikipedia.org/wiki/Builder_pattern">Builder Pattern</a>
 */
public class BuilderDemo {
    
    public static void main(String[] args) {
        System.out.println("=== BUILDER PATTERN DEMONSTRATIONS ===\n");
        
        // 1. Basic Builder - readable construction
        System.out.println("1. BASIC BUILDER:");
        User user = new User.Builder("John", "Doe")
            .age(30)
            .phone("555-1234")
            .address("123 Main St")
            .emailNotifications(true)
            .marketingEmails(true)
            .build();
        System.out.println(user);
        System.out.println();
        
        // 2. Builder with Validation
        System.out.println("2. BUILDER WITH VALIDATION:");
        try {
            Computer invalid = new Computer.Builder("Intel i9", "32GB")
                .gpu("RTX 4090")
                .powerSupply("500W") // Too weak!
                .build();
        } catch (IllegalStateException e) {
            System.out.println("Validation caught error: " + e.getMessage());
        }
        
        Computer valid = new Computer.Builder("Intel i9", "32GB")
            .gpu("RTX 4090")
            .powerSupply("1000W")
            .storage("2TB SSD")
            .liquidCooling(true)
            .build();
        System.out.println("Valid computer: " + valid);
        System.out.println();
        
        // 3. Builder with Inheritance
        System.out.println("3. BUILDER WITH INHERITANCE:");
        NYStylePizza nyPizza = new NYStylePizza.Builder()
            .name("Brooklyn Special")
            .dough("Thin Crust")
            .addTopping("Pepperoni")
            .addTopping("Mushrooms")
            .extraCheese(true)
            .build();
        System.out.println(nyPizza);
        
        Calzone calzone = new Calzone.Builder()
            .name("Meat Lover's Calzone")
            .addTopping("Sausage")
            .addTopping("Bacon")
            .sauceInside(true)
            .build();
        System.out.println(calzone);
        System.out.println();
        
        // 4. Director for common configurations
        System.out.println("4. DIRECTOR PATTERN:");
        Director director = new Director();
        
        Computer gamingPC = director.buildGamingPC(
            new Computer.Builder("AMD Ryzen 9", "64GB")
        );
        System.out.println("Gaming PC: " + gamingPC);
        
        Computer officePC = director.buildOfficePC(
            new Computer.Builder("Intel i5", "16GB")
        );
        System.out.println("Office PC: " + officePC);
        
        Pizza margherita = director.buildMargherita(new NYStylePizza.Builder());
        System.out.println("Margherita: " + margherita);
        System.out.println();
        
        // 5. Step-by-step construction
        System.out.println("5. STEP-BY-STEP (Same builder, different products):");
        Computer.Builder builder = new Computer.Builder("AMD Ryzen 7", "32GB");
        
        // Can reuse builder or add conditionally
        Computer midRange = builder
            .gpu("RTX 3060")
            .powerSupply("650W")
            .build();
        System.out.println("Mid-range: " + midRange);
        
        System.out.println("\n=== BENEFITS ===");
        System.out.println("1. No telescoping constructors");
        System.out.println("2. Named parameters via method chaining");
        System.out.println("3. Immutable objects (no setters needed)");
        System.out.println("4. Validation at build time");
        System.out.println("5. Readable code at call site");
        System.out.println("6. Supports inheritance hierarchies");
    }
}
