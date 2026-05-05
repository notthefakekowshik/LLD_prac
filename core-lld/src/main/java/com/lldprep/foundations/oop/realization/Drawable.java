// REALIZATION: Drawable is a pure contract (interface).
// Any class that can be drawn implements (realizes) this interface.
// No state, no implementation — only the promise.
// Relationship: Circle - - -|> Drawable  (dashed line + open arrowhead = realizes)
package com.lldprep.foundations.oop.realization;

public interface Drawable {
    void draw();
    double area();
}
