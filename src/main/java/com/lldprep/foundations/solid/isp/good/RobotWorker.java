// ISP GOOD: Robot implements ONLY Workable — exactly what it can support.
// No UnsupportedOperationException, no dead stubs, no surprises.
// Clients that need Eatable or Restable simply do not use RobotWorker.
package com.lldprep.foundations.solid.isp.good;

public class RobotWorker implements Workable {

    private final String id;

    public RobotWorker(String id) {
        this.id = id;
    }

    @Override
    public void work() {
        System.out.println("Robot " + id + " is working.");
    }
}
