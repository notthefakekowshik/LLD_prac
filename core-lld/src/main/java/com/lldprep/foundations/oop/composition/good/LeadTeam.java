package com.lldprep.foundations.oop.composition.good;

/**
 * Concrete management behavior: Leading a development team.
 * Used by Managers and Tech Leads.
 */
public class LeadTeam implements ManageBehavior {
    @Override
    public void manageTeam(String employeeName) {
        System.out.println("  [MANAGE] " + employeeName + " is leading the engineering team, mentoring juniors.");
    }
}
