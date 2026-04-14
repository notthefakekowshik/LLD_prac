package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * ABSTRACT FACTORY PATTERN
 * =========================
 *
 * WHY IT EXISTS:
 * Creates families of related objects without specifying concrete classes.
 * Ensures that products from the same family are used together.
 *
 * PROBLEMS IT SOLVES:
 * - Mixing UI components from different platforms (Windows button with Mac scrollbar)
 * - Hardcoding platform-specific code throughout application
 * - Difficulty supporting multiple product families (themes, platforms, databases)
 * - Ensuring consistency within a product family
 *
 * WHEN TO USE:
 * - Cross-platform UI (Windows, Mac, Linux, Mobile)
 * - Multiple database support (MySQL, PostgreSQL, MongoDB)
 * - Theme systems (Dark, Light, High Contrast)
 * - Different look-and-feels in GUI frameworks
 * - Supporting multiple cloud providers (AWS, Azure, GCP)
 *
 * KEY BENEFIT:
 * Guarantees that products from the same family work together.
 * You can't accidentally mix Windows button with Mac checkbox.
 *
 * COMPARISON WITH FACTORY:
 * - Factory: Creates ONE type of object
 * - Abstract Factory: Creates FAMILIES of related objects
 *
 * REAL-WORLD EXAMPLES:
 * - Java AWT Toolkit (createButton, createWindow)
 * - JDBC Connection, Statement, ResultSet families
 * - Spring's FactoryBean hierarchies
 * - UI frameworks (Flutter, React Native) for different platforms
 *
 * @see <a href="https://en.wikipedia.org/wiki/Abstract_factory_pattern">Abstract Factory Pattern</a>
 */
public class AbstractFactoryDemo {
    
    public static void main(String[] args) {
        System.out.println("=== ABSTRACT FACTORY PATTERN ===\n");
        
        // Configure application for Windows
        System.out.println("1. WINDOWS APPLICATION:");
        GUIFactory windowsFactory = FactoryProvider.getFactory(FactoryProvider.Platform.WINDOWS);
        Application windowsApp = new Application(windowsFactory);
        windowsApp.renderUI();
        windowsApp.userInteraction();
        
        // Configure application for Mac (same code!)
        System.out.println("2. MAC APPLICATION:");
        GUIFactory macFactory = FactoryProvider.getFactory(FactoryProvider.Platform.MAC);
        Application macApp = new Application(macFactory);
        macApp.renderUI();
        macApp.userInteraction();
        
        System.out.println("=== KEY BENEFITS ===");
        System.out.println("1. GUARANTEED consistency - no mixed platform UI");
        System.out.println("2. Easy to add new platform (LinuxGUIFactory)");
        System.out.println("3. Client code unchanged when switching platforms");
        System.out.println("4. Single decision point for platform selection");
    }
}
