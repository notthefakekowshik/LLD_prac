package com.lldprep.foundations.oop.composition.good;

/**
 * Work behavior interface — Strategy pattern for how an employee performs work.
 * Using composition, an Employee CAN do work, rather than IS-A worker.
 */
public interface WorkBehavior {
    void doWork(String employeeName);
}
