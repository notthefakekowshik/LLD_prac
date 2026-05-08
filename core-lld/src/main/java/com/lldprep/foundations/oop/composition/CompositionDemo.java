package com.lldprep.foundations.oop.composition;

import com.lldprep.foundations.oop.composition.bad.TechLead;
import com.lldprep.foundations.oop.composition.good.*;

/**
 * Demonstrates COMPOSITION over INHERITANCE using Employee roles.
 *
 * <p>Problem: An employee can have multiple responsibilities (coding, managing).
 * Java doesn't allow multiple inheritance, so composition is the only clean solution.
 */
public class CompositionDemo {

    public static void main(String[] args) {
        System.out.println("===== COMPOSITION OVER INHERITANCE =====\n");
        System.out.println("Scenario: Employees with different roles (Developer, Manager, Tech Lead)\n");

        // --- BAD VERSION ---
        System.out.println("--- BAD: Work and management logic are copy-pasted ---");
        System.out.println("Problem: if work() changes, every copy (Developer, TechLead, Manager...) must be updated.");
        TechLead badTechLead = new TechLead("Alice");
        badTechLead.work();
        badTechLead.manage();
        System.out.println("Also: Developer has identical work() code — maintenance nightmare.");

        System.out.println();

        // --- GOOD VERSION ---
        System.out.println("--- GOOD: Behaviours are composed via injection ---");

        // Developer: codes, doesn't manage
        Employee developer = new Employee("Bob", "Developer",
            new CodeWork(), new NoManage());

        // Manager: plans, leads team
        Employee manager = new Employee("Carol", "Manager",
            new ReviewWork(), new LeadTeam());

        // Tech Lead: codes AND leads team (composition allows both!)
        Employee techLead = new Employee("Alice", "Tech Lead",
            new CodeWork(), new LeadTeam());

        developer.performWork();
        developer.performManagement();
        System.out.println();

        manager.performWork();
        manager.performManagement();
        System.out.println();

        techLead.performWork();
        techLead.performManagement();

        System.out.println();
        System.out.println("Key insight: CodeWork exists in ONE place and is shared by Developer and TechLead.");
        System.out.println("Changing coding standards requires editing ONLY CodeWork.");

        System.out.println();

        // --- RUNTIME BEHAVIOUR SWAP ---
        System.out.println("--- RUNTIME SWAP: Bob promoted to Tech Lead ---");
        System.out.println("Before: " + developer.getTitle());
        developer.performManagement();

        developer.setManageBehavior(new LeadTeam());  // Runtime promotion!
        System.out.println("After promotion:");
        developer.performManagement();

        System.out.println();
        System.out.println("--- RUNTIME SWAP: Tech Lead becomes pure Manager ---");
        techLead.setWorkBehavior(new ReviewWork());  // Stop coding, focus on planning
        techLead.performWork();

        System.out.println();

        // --- TRUE COMPOSITION (Strict UML) ---
        System.out.println("--- TRUE COMPOSITION: Car OWNS Engine internally ---");
        System.out.println("Engine is created INSIDE Car's constructor. It cannot be swapped.");
        System.out.println("When Car is destroyed, Engine is destroyed too.");

        com.lldprep.foundations.oop.composition.car.Car sedan =
            new com.lldprep.foundations.oop.composition.car.Car("Toyota Camry", "V6 Hybrid");
        sedan.start();
        sedan.stop();

        System.out.println();
        System.out.println("Key insight: Car does NOT accept Engine via constructor.");
        System.out.println("Engine is created with 'new Engine()' inside Car's constructor.");
        System.out.println("No setter, no swap — strict ownership. Relationship: Car ◆----> Engine");

        System.out.println("\n===== END COMPOSITION DEMO =====");
    }
}
