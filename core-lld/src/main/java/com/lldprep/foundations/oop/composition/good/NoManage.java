package com.lldprep.foundations.oop.composition.good;

/**
 * Null object pattern for management behavior.
 * Used by individual contributors who do not manage anyone.
 */
public class NoManage implements ManageBehavior {
    @Override
    public void manageTeam(String employeeName) {
        System.out.println("  [MANAGE] " + employeeName + " is an IC (no direct reports).");
    }
}
