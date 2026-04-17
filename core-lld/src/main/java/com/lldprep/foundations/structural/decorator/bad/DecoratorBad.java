package com.lldprep.foundations.structural.decorator.bad;

/**
 * BAD: Adding features via subclass explosion.
 *
 * Every combination of features requires its own subclass:
 *   Coffee, CoffeeWithMilk, CoffeeWithSugar, CoffeeWithMilkAndSugar,
 *   Espresso, EspressoWithMilk, EspressoWithVanilla ... → combinatorial explosion.
 *
 * Problems:
 * 1. M beverages × N add-ons = M×N subclasses.
 * 2. Features cannot be combined at runtime — the class is fixed at compile time.
 * 3. Adding a new add-on (e.g., Caramel) requires editing every existing beverage subclass.
 * 4. OCP violation — each new combination requires a new class or modifying existing ones.
 */
public class DecoratorBad {

    public static void main(String[] args) {
        System.out.println(new CoffeeWithMilkAndSugar().getDescription()
                + " $" + new CoffeeWithMilkAndSugar().getCost());
    }
}

class Coffee {
    public String getDescription() { return "Coffee"; }
    public double getCost() { return 1.00; }
}

class CoffeeWithMilk extends Coffee {
    @Override
    public String getDescription() { return "Coffee + Milk"; }
    @Override
    public double getCost() { return 1.25; }
}

class CoffeeWithSugar extends Coffee {
    @Override
    public String getDescription() { return "Coffee + Sugar"; }
    @Override
    public double getCost() { return 1.10; }
}

// Need a new class for EVERY combination — this does not scale.
class CoffeeWithMilkAndSugar extends Coffee {
    @Override
    public String getDescription() { return "Coffee + Milk + Sugar"; }
    @Override
    public double getCost() { return 1.35; }
}
