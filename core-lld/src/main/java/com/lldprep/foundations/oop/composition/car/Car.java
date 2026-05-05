// COMPOSITION (TRUE): Car HAS-A Engine via INTERNAL CREATION.
// Engine is NOT injected — it is created inside Car's constructor.
// Engine cannot be swapped at runtime. Engine dies when Car dies.
// This is the strict UML definition of composition.
package com.lldprep.foundations.oop.composition.car;

public class Car {

    private final String model;
    private final Engine engine;  // composition — created internally, tight ownership

    public Car(String model, String engineType) {
        this.model = model;
        this.engine = new Engine(engineType);  // <-- INTERNAL CREATION, no injection
    }

    public void start() {
        System.out.println("Car [" + model + "] starting...");
        engine.start();
    }

    public void stop() {
        System.out.println("Car [" + model + "] stopping...");
        engine.stop();
    }

    // Note: NO setEngine() method — engine cannot be swapped. This is strong ownership.

    public String getModel() {
        return model;
    }

    public String getEngineType() {
        return engine.getType();
    }
}
