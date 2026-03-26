// OCP GOOD: Open for extension — new shapes implement this interface without touching existing code.
// Closed for modification — AreaCalculator and existing shapes never change when a new shape is added.
package com.lldprep.foundations.solid.ocp.good;

public interface Shape {
    double area();
    String name();
}
