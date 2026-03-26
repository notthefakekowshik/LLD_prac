package com.lldprep.foundations.solid.isp;

import com.lldprep.foundations.solid.isp.bad.HumanWorker;
import com.lldprep.foundations.solid.isp.bad.RobotWorker;
import com.lldprep.foundations.solid.isp.bad.Worker;
import com.lldprep.foundations.solid.isp.good.Eatable;
import com.lldprep.foundations.solid.isp.good.Restable;
import com.lldprep.foundations.solid.isp.good.Workable;

public class ISPDemo {

    public static void main(String[] args) {
        System.out.println("===== ISP: INTERFACE SEGREGATION PRINCIPLE =====\n");

        // --- BAD VERSION ---
        System.out.println("--- BAD: Fat interface forces Robot to implement eat() and sleep() ---");

        Worker badHuman = new HumanWorker("Alice");
        badHuman.work();
        badHuman.eat("pasta");
        badHuman.sleep(8);

        System.out.println();
        Worker badRobot = new RobotWorker("R2D2");
        badRobot.work(); // fine

        System.out.println("Calling eat() on robot — this will CRASH:");
        try {
            badRobot.eat("oil"); // boom!
        } catch (UnsupportedOperationException e) {
            System.out.println("  CAUGHT: " + e.getMessage());
        }

        System.out.println("Calling sleep() on robot — this will CRASH:");
        try {
            badRobot.sleep(8); // boom!
        } catch (UnsupportedOperationException e) {
            System.out.println("  CAUGHT: " + e.getMessage());
        }

        System.out.println();

        // --- GOOD VERSION ---
        System.out.println("--- GOOD: Segregated interfaces — Robot only implements Workable ---");

        com.lldprep.foundations.solid.isp.good.HumanWorker goodHuman =
            new com.lldprep.foundations.solid.isp.good.HumanWorker("Bob");
        com.lldprep.foundations.solid.isp.good.RobotWorker goodRobot =
            new com.lldprep.foundations.solid.isp.good.RobotWorker("C3PO");

        // Human satisfies all three interfaces
        Workable w = goodHuman;
        Eatable e = goodHuman;
        Restable r = goodHuman;
        w.work();
        e.eat("sandwich");
        r.sleep(7);

        System.out.println();

        // Robot only satisfies Workable — clean, no exceptions, no dead stubs
        goodRobot.work();
        System.out.println("Robot has no eat() or sleep() — no crashes, no stubs, no surprises.");

        System.out.println("\n===== END ISP DEMO =====");
    }
}
