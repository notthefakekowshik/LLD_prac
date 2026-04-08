// ISP GOOD: Focused interface — only clients that need eating behaviour depend on this.
// Humans implement this; robots do not — no forced stubs or exceptions.
package com.lldprep.foundations.solid.isp.good;

public interface Eatable {
    void eat(String food);
}
