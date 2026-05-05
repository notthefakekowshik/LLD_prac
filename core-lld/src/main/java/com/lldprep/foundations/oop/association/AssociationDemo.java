package com.lldprep.foundations.oop.association;

public class AssociationDemo {

    public static void main(String[] args) {
        System.out.println("===== ASSOCIATION =====\n");

        // Teacher is created independently — it does NOT belong to any Department
        Teacher alice = new Teacher("Alice", "Mathematics");
        Teacher bob = new Teacher("Bob", "Physics");

        // Department gets the Teacher injected — no ownership
        Department mathDept = new Department("Math Dept", alice);
        Department scienceDept = new Department("Science Dept", bob);

        // One Teacher can even be shared across Departments
        Department eliteDept = new Department("Elite Dept", alice);

        mathDept.conductClass();
        scienceDept.conductClass();

        System.out.println();
        System.out.println("--- Alice teaches in TWO departments (shared reference) ---");
        eliteDept.conductClass();

        System.out.println();
        System.out.println("Key insight: Alice exists independently of any Department.");
        System.out.println("Destroying mathDept does NOT destroy Alice.");
        System.out.println("Relationship type: Department ----> Teacher  (uses-a, no ownership)");
        System.out.println("\n===== END ASSOCIATION DEMO =====");
    }
}
