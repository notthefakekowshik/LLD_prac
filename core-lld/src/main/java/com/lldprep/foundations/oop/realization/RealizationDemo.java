package com.lldprep.foundations.oop.realization;

import java.util.List;

public class RealizationDemo {

    public static void main(String[] args) {
        System.out.println("===== REALIZATION =====\n");

        // Refer to shapes via the interface — caller doesn't care about the concrete type
        List<Drawable> shapes = List.of(
            new Circle(5.0),
            new Rectangle(4.0, 6.0),
            new Circle(3.0)
        );

        shapes.forEach(shape -> {
            shape.draw();
            System.out.printf("  Area: %.2f%n", shape.area());
        });

        System.out.println();
        System.out.println("Key insight: The caller only knows about Drawable.");
        System.out.println("Circle and Rectangle can change their HOW without breaking the caller.");
        System.out.println("Adding a Triangle requires zero changes to this demo — just implement Drawable.");
        System.out.println("Relationship type: Circle - - -|> Drawable  (realizes/implements)");
        System.out.println("\n===== END REALIZATION DEMO =====");
    }
}
