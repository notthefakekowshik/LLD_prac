package com.lldprep.foundations.creational.prototype.good;

/**
 * Prototype Registry Pattern - store and clone prototypes.
 * 
 * USE CASE: When you need many similar objects with slight variations.
 * Store prototypical instances, clone and customize as needed.
 */
public abstract class Shape implements Prototype {
    protected int x;
    protected int y;
    protected String color;
    
    public Shape() {}
    
    public Shape(Shape source) {
        this.x = source.x;
        this.y = source.y;
        this.color = source.color;
    }
    
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public void setColor(String color) { this.color = color; }
    
    public abstract void draw();
    
    @Override
    public abstract Shape clone();
}

/**
 * Concrete Prototype - Circle.
 */
class Circle extends Shape {
    private int radius;
    
    public Circle() {}
    
    public Circle(Circle source) {
        super(source);
        this.radius = source.radius;
    }
    
    public void setRadius(int radius) { this.radius = radius; }
    
    @Override
    public void draw() {
        System.out.println("Drawing Circle at (" + x + "," + y + ") radius=" + radius + " color=" + color);
    }
    
    @Override
    public Circle clone() {
        return new Circle(this);  // Use copy constructor
    }
}

/**
 * Concrete Prototype - Rectangle.
 */
class Rectangle extends Shape {
    private int width;
    private int height;
    
    public Rectangle() {}
    
    public Rectangle(Rectangle source) {
        super(source);
        this.width = source.width;
        this.height = source.height;
    }
    
    public void setWidth(int width) { this.width = width; }
    public void setHeight(int height) { this.height = height; }
    
    @Override
    public void draw() {
        System.out.println("Drawing Rectangle at (" + x + "," + y + ") " + width + "x" + height + " color=" + color);
    }
    
    @Override
    public Rectangle clone() {
        return new Rectangle(this);
    }
}
