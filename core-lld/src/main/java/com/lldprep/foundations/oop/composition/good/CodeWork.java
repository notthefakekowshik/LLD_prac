package com.lldprep.foundations.oop.composition.good;

/**
 * Concrete work behavior: Writing code.
 * Used by Developers and Tech Leads.
 */
public class CodeWork implements WorkBehavior {
    @Override
    public void doWork(String employeeName) {
        System.out.println("  [WORK] " + employeeName + " is writing and reviewing code.");
    }
}
