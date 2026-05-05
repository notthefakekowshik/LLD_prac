package com.lldprep.foundations.oop.aggregation;

import java.util.List;

public class AggregationDemo {

    public static void main(String[] args) {
        System.out.println("===== AGGREGATION =====\n");

        // Players exist independently — created before any Team
        Player p1 = new Player("Rohit");
        Player p2 = new Player("Virat");
        Player p3 = new Player("Dhoni");

        // Team aggregates Players — it does NOT own them
        Team india = new Team("India", List.of(p1, p2, p3));
        india.startMatch();

        System.out.println();
        System.out.println("--- Players can be re-used in another team ---");
        Team allStars = new Team("All Stars", List.of(p2, p3));
        allStars.startMatch();

        System.out.println();
        System.out.println("Key insight: 'india' team object can go out of scope —");
        System.out.println("  Rohit, Virat, Dhoni still exist as independent objects.");
        System.out.println("Relationship type: Team <>----> Player  (weak ownership, whole-part)");
        System.out.println("\n===== END AGGREGATION DEMO =====");
    }
}
