// OCP VIOLATION: Adding a new shape requires editing this class.
// Every new shape type (hexagon, pentagon, etc.) forces a change to this switch/if-else,
// violating "open for extension, closed for modification."
package com.lldprep.foundations.solid.ocp.bad;

public class ShapeAreaCalculator {

    public double calculateArea(String shapeType, double... dims) {
        if (shapeType.equalsIgnoreCase("circle")) {
            double radius = dims[0];
            return Math.PI * radius * radius;
        } else if (shapeType.equalsIgnoreCase("rectangle")) {
            double width = dims[0];
            double height = dims[1];
            return width * height;
        } else if (shapeType.equalsIgnoreCase("triangle")) {
            double base = dims[0];
            double height = dims[1];
            return 0.5 * base * height;
        }
        // To add Hexagon: YOU MUST EDIT THIS CLASS — that's the violation.
        throw new IllegalArgumentException("Unknown shape: " + shapeType);
    }
}
