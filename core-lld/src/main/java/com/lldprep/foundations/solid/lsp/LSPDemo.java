package com.lldprep.foundations.solid.lsp;

import com.lldprep.foundations.solid.lsp.bad.Rectangle;
import com.lldprep.foundations.solid.lsp.bad.Square;
import com.lldprep.foundations.solid.lsp.good.Shape;

public class LSPDemo {

    // This method is written against the Rectangle contract:
    // "I can set width and height independently and area() will equal width * height."
    private static void printExpectedArea(Rectangle rect, int w, int h) {
        rect.setWidth(w);
        rect.setHeight(h);
        int expected = w * h;
        int actual = rect.area();
        System.out.println("  Set width=" + w + ", height=" + h
            + " | Expected area=" + expected
            + " | Actual area=" + actual
            + " | Correct: " + (expected == actual));
    }

    public static void main(String[] args) {
        System.out.println("===== LSP: LISKOV SUBSTITUTION PRINCIPLE =====\n");

        // --- BAD VERSION ---
        System.out.println("--- BAD: Square extending Rectangle breaks the setter contract ---");

        Rectangle badRect = new Rectangle(3, 4);
        System.out.print("Using Rectangle: ");
        printExpectedArea(badRect, 3, 4);

        // Substituting Square for Rectangle — the code BREAKS silently
        Rectangle badSquare = new Square(5); // polymorphic reference
        System.out.print("Using Square as Rectangle: ");
        printExpectedArea(badSquare, 3, 4); // setWidth(3) also sets height=3 → area = 9, not 12!

        System.out.println("  ^^^ Square silently gave wrong area! This is the LSP violation.\n");

        // --- GOOD VERSION ---
        System.out.println("--- GOOD: Rectangle and Square are independent — no broken contracts ---");

        com.lldprep.foundations.solid.lsp.good.Rectangle goodRect =
            new com.lldprep.foundations.solid.lsp.good.Rectangle(3.0, 4.0);
        com.lldprep.foundations.solid.lsp.good.Square goodSquare =
            new com.lldprep.foundations.solid.lsp.good.Square(5.0);

        Shape[] shapes = {goodRect, goodSquare};
        for (Shape s : shapes) {
            System.out.println("  " + s.name() + " -> area = " + s.area());
        }

        // Both uphold their own contracts via the Shape interface — no surprises
        goodRect.setWidth(6.0);
        goodRect.setHeight(7.0);
        System.out.println("  After setWidth(6), setHeight(7): " + goodRect.name() + " -> area = " + goodRect.area());

        goodSquare.setSide(8.0);
        System.out.println("  After setSide(8): " + goodSquare.name() + " -> area = " + goodSquare.area());

        System.out.println("\n===== END LSP DEMO =====");
    }
}
