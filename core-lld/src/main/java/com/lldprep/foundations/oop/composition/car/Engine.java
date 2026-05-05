// COMPOSITION (TRUE): Engine is OWNED by Car.
// Engine is created INSIDE Car's constructor — it cannot exist independently.
// If Car is destroyed, Engine is destroyed with it.
// Relationship: Car ◆----> Engine  (filled diamond = composition)
package com.lldprep.foundations.oop.composition.car;

public class Engine {

    private final String type;
    private boolean running;

    public Engine(String type) {
        this.type = type;
        this.running = false;
    }

    public void start() {
        this.running = true;
        System.out.println("  Engine [" + type + "] started.");
    }

    public void stop() {
        this.running = false;
        System.out.println("  Engine [" + type + "] stopped.");
    }

    public boolean isRunning() {
        return running;
    }

    public String getType() {
        return type;
    }
}
