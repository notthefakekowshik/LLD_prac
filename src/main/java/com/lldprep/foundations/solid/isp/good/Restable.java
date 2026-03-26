// ISP GOOD: Focused interface — only clients that need rest behaviour depend on this.
// Humans implement this; robots do not — no forced stubs or exceptions.
package com.lldprep.foundations.solid.isp.good;

public interface Restable {
    void sleep(int hours);
}
