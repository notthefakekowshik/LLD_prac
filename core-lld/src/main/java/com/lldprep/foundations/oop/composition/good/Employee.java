package com.lldprep.foundations.oop.composition.good;

/**
 * Employee class using COMPOSITION instead of inheritance.
 *
 * <p>An Employee CAN work and CAN manage — but is not defined by being a "Worker" or "Manager".
 * This allows any combination:
 * <ul>
 *   <li>Developer: CodeWork + NoManage</li>
 *   <li>Manager: ReviewWork + LeadTeam</li>
 *   <li>TechLead: CodeWork + LeadTeam</li>
 * </ul>
 *
 * <p>Behaviors can be swapped at runtime (promotion, role change).
 */
public class Employee {
    private final String name;
    private final String title;

    // Composed behaviors — injected via constructor
    private WorkBehavior workBehavior;
    private ManageBehavior manageBehavior;

    public Employee(String name, String title,
                    WorkBehavior workBehavior,
                    ManageBehavior manageBehavior) {
        this.name = name;
        this.title = title;
        this.workBehavior = workBehavior;
        this.manageBehavior = manageBehavior;
    }

    public void performWork() {
        System.out.println("[" + title + "] " + name + " working:");
        workBehavior.doWork(name);
    }

    public void performManagement() {
        System.out.println("[" + title + "] " + name + " managing:");
        manageBehavior.manageTeam(name);
    }

    // Runtime behavior swap — promotion, reassignment, etc.
    public void setWorkBehavior(WorkBehavior workBehavior) {
        this.workBehavior = workBehavior;
    }

    public void setManageBehavior(ManageBehavior manageBehavior) {
        this.manageBehavior = manageBehavior;
    }

    public String getName() { return name; }
    public String getTitle() { return title; }
}
