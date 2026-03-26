package com.lldprep.foundations.solid.ocp;

import com.lldprep.foundations.solid.ocp.bad.ShapeAreaCalculator;
import com.lldprep.foundations.solid.ocp.good.*;

import java.util.List;

public class OCPDemo {

    public static void main(String[] args) {
        System.out.println("===== OCP: OPEN/CLOSED PRINCIPLE =====\n");

        // --- BAD VERSION ---
        System.out.println("--- BAD: Must edit ShapeAreaCalculator to add any new shape ---");
        ShapeAreaCalculator badCalc = new ShapeAreaCalculator();
        System.out.println("Circle area:    " + badCalc.calculateArea("circle", 5.0));
        System.out.println("Rectangle area: " + badCalc.calculateArea("rectangle", 4.0, 6.0));
        System.out.println("Triangle area:  " + badCalc.calculateArea("triangle", 3.0, 8.0));
        System.out.println("Problem: adding Hexagon requires opening ShapeAreaCalculator and editing it.");

        System.out.println();

        // --- GOOD VERSION ---
        System.out.println("--- GOOD: AreaCalculator never changes — new shapes just implement Shape ---");
        AreaCalculator calc = new AreaCalculator();

        List<Shape> shapes = List.of(
            new Circle(5.0),
            new Rectangle(4.0, 6.0),
            new Triangle(3.0, 8.0)
        );

        for (Shape s : shapes) {
            System.out.printf("%-30s area = %.4f%n", s.name(), s.area());
        }
        System.out.printf("Total area: %.4f%n", calc.totalArea(shapes));

        System.out.println();

        // --- PROVING OCP: Add Hexagon inline without touching ANY existing class ---
        System.out.println("--- PROVING OCP: Adding Hexagon — zero changes to existing classes ---");

        // Anonymous inner class acting as a brand-new shape extension
        Shape hexagon = new Shape() {
            private final double side = 4.0;

            @Override
            public double area() {
                // Regular hexagon: (3 * sqrt(3) / 2) * side^2
                return (3.0 * Math.sqrt(3.0) / 2.0) * side * side;
            }

            @Override
            public String name() {
                return "Hexagon(side=" + side + ")";
            }
        };

        List<Shape> extended = List.of(new Circle(5.0), new Rectangle(4.0, 6.0), hexagon);
        for (Shape s : extended) {
            System.out.printf("%-30s area = %.4f%n", s.name(), s.area());
        }
        System.out.printf("Total area (with Hexagon): %.4f%n", calc.totalArea(extended));

        System.out.println("\n===== END OCP DEMO =====");
    }
}
