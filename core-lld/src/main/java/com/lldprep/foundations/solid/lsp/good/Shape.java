// LSP GOOD: A minimal abstraction that both Rectangle and Square can honour fully.
// No setter contracts that only apply to one shape. Each implementor upholds its own contract.
package com.lldprep.foundations.solid.lsp.good;

public interface Shape {
    double area();
    String name();
}
