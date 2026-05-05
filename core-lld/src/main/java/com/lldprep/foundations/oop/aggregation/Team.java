// AGGREGATION: Team HAS-A list of Players, but does NOT own them.
// Players are created outside and passed in (whole-part, but weak ownership).
// If Team is dissolved, Players still exist — they can join another team.
// Relationship: Team <>----> Player  (open diamond = aggregation)
package com.lldprep.foundations.oop.aggregation;

import java.util.List;

public class Team {

    private final String teamName;
    private final List<Player> players;   // aggregation — list passed in, not created here

    public Team(String teamName, List<Player> players) {
        this.teamName = teamName;
        this.players = players;
    }

    public void startMatch() {
        System.out.println("Team [" + teamName + "] starting match with players:");
        players.forEach(p -> System.out.println("  - " + p.getName()));
    }

    public int size() { return players.size(); }
}
