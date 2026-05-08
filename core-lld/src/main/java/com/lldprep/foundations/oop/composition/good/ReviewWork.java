package com.lldprep.foundations.oop.composition.good;

/**
 * Concrete work behavior: Strategic review and planning.
 * Used by Managers who focus on planning over hands-on coding.
 */
public class ReviewWork implements WorkBehavior {
    @Override
    public void doWork(String employeeName) {
        System.out.println("  [WORK] " + employeeName + " is reviewing project plans and reports.");
    }
}
