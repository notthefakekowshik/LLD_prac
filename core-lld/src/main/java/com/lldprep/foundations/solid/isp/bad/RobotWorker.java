// ISP VIOLATION: Robot is forced to implement methods it cannot support.
// Robots can work, but have no concept of eating or sleeping.
// This forces dummy/exception-throwing implementations — a clear sign of ISP violation.
// Any caller iterating over Worker objects and calling eat() or sleep() will get a runtime crash.
package com.lldprep.foundations.solid.isp.bad;

public class RobotWorker implements Worker {

    private final String id;

    public RobotWorker(String id) {
        this.id = id;
    }

    @Override
    public void work() {
        System.out.println("Robot " + id + " is working.");
    }

    // ISP VIOLATION: Robot cannot eat — forced stub that breaks at runtime
    @Override
    public void eat(String food) {
        throw new UnsupportedOperationException("Robot " + id + " cannot eat!");
    }

    // ISP VIOLATION: Robot cannot sleep — forced stub that breaks at runtime
    @Override
    public void sleep(int hours) {
        throw new UnsupportedOperationException("Robot " + id + " cannot sleep!");
    }
}
