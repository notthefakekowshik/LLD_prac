package com.lldprep.foundations.creational.builder.good;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder with Inheritance - hierarchical builders.
 * 
 * Pattern: Each subclass builder extends parent builder
 * with recursive generic type for method chaining.
 */
public abstract class Pizza {
    protected final String name;
    protected final String dough;
    protected final String sauce;
    protected final List<String> toppings;
    
    protected Pizza(Builder<?> builder) {
        this.name = builder.name;
        this.dough = builder.dough;
        this.sauce = builder.sauce;
        this.toppings = new ArrayList<>(builder.toppings);
    }
    
    @Override
    public String toString() {
        return name + " [dough=" + dough + ", sauce=" + sauce + ", toppings=" + toppings + "]";
    }
    
    /**
     * Generic Builder with recursive type parameter.
     * Allows subclass builders to chain methods properly.
     */
    public abstract static class Builder<T extends Builder<T>> {
        private String name = "Custom Pizza";
        private String dough = "Regular";
        private String sauce = "Tomato";
        private List<String> toppings = new ArrayList<>();
        
        protected abstract T self(); // Return this as subclass type
        
        public T name(String name) {
            this.name = name;
            return self();
        }
        
        public T dough(String dough) {
            this.dough = dough;
            return self();
        }
        
        public T sauce(String sauce) {
            this.sauce = sauce;
            return self();
        }
        
        public T addTopping(String topping) {
            this.toppings.add(topping);
            return self();
        }
        
        public abstract Pizza build();
    }
}

/**
 * NYStylePizza with its own builder.
 */
class NYStylePizza extends Pizza {
    private final boolean extraCheese;
    private final boolean foldable;
    
    private NYStylePizza(Builder builder) {
        super(builder);
        this.extraCheese = builder.extraCheese;
        this.foldable = builder.foldable;
    }
    
    @Override
    public String toString() {
        return super.toString() + " [NY Style, extraCheese=" + extraCheese + ", foldable=" + foldable + "]";
    }
    
    public static class Builder extends Pizza.Builder<Builder> {
        private boolean extraCheese = false;
        private boolean foldable = true; // NY style is always foldable
        
        @Override
        protected Builder self() {
            return this;
        }
        
        public Builder extraCheese(boolean enabled) {
            this.extraCheese = enabled;
            return this;
        }
        
        @Override
        public NYStylePizza build() {
            return new NYStylePizza(this);
        }
    }
}

/**
 * Calzone with its own builder.
 */
class Calzone extends Pizza {
    private final boolean sauceInside;
    
    private Calzone(Builder builder) {
        super(builder);
        this.sauceInside = builder.sauceInside;
    }
    
    @Override
    public String toString() {
        return super.toString() + " [Calzone, sauceInside=" + sauceInside + "]";
    }
    
    public static class Builder extends Pizza.Builder<Builder> {
        private boolean sauceInside = false;
        
        @Override
        protected Builder self() {
            return this;
        }
        
        public Builder sauceInside(boolean inside) {
            this.sauceInside = inside;
            return this;
        }
        
        @Override
        public Calzone build() {
            return new Calzone(this);
        }
    }
}
