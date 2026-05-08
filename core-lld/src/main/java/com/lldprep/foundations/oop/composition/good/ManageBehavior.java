package com.lldprep.foundations.oop.composition.good;

/**
 * Management behavior interface — Strategy pattern for how an employee manages people.
 * Using composition, an Employee CAN manage, rather than IS-A manager.
 */
public interface ManageBehavior {
    void manageTeam(String employeeName);
}
