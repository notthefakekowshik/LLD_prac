// ISP GOOD: Focused interface — only clients that need work() depend on this.
// Robots implement only this; they are not burdened by eat/sleep contracts.
package com.lldprep.foundations.solid.isp.good;

public interface Workable {
    void work();
}
