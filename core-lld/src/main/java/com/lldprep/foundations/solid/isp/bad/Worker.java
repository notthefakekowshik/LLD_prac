// ISP VIOLATION: Fat interface forces ALL implementors to provide work, eat, and sleep.
// Robots can work but cannot eat or sleep — yet they are forced to implement those methods.
// Clients that only need work() are still coupled to eat() and sleep().
package com.lldprep.foundations.solid.isp.bad;

public interface Worker {
    void work();
    void eat(String food);
    void sleep(int hours);
}
