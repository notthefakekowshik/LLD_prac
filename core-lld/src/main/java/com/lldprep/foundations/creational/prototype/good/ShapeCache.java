package com.lldprep.foundations.creational.prototype.good;

import java.util.HashMap;
import java.util.Map;

/**
 * Prototype Registry - cache of prototypical instances.
 * 
 * Client asks registry for clone of registered prototype.
 * Avoids subclass-specific creation logic in client.
 */
public class ShapeCache {
    private Map<String, Shape> cache = new HashMap<>();
    
    public ShapeCache() {
        // Register prototypical instances
        Circle circle = new Circle();
        circle.setX(0);
        circle.setY(0);
        circle.setRadius(10);
        circle.setColor("Red");
        cache.put("CIRCLE", circle);
        
        Rectangle rectangle = new Rectangle();
        rectangle.setX(0);
        rectangle.setY(0);
        rectangle.setWidth(20);
        rectangle.setHeight(10);
        rectangle.setColor("Blue");
        cache.put("RECTANGLE", rectangle);
    }
    
    /**
     * Get a clone of the registered prototype.
     * Client gets fresh copy to customize.
     */
    public Shape get(String id) {
        Shape prototype = cache.get(id.toUpperCase());
        if (prototype == null) {
            throw new IllegalArgumentException("Unknown shape: " + id);
        }
        return prototype.clone();
    }
    
    public void register(String id, Shape shape) {
        cache.put(id.toUpperCase(), shape);
    }
}
