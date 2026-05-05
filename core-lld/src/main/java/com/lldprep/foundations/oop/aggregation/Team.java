// AGGREGATION: Team HAS-A list of Players, but does NOT own them.
// Players are created outside and passed in (whole-part, but weak ownership).
// If Team is dissolved, Players still exist — they can join another team.
// Relationship: Team <>----> Player  (open diamond = aggregation)
package com.lldprep.foundations.oop.aggregation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Team {

    private final String teamName;
    private final List<Player> players;   // aggregation — defensive copy, not direct reference

    public Team(String teamName, List<Player> players) {
        this.teamName = teamName;
        this.players = new ArrayList<>(players);  // defensive copy — protects internal state
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);  // prevent external mutation
    }

    public void startMatch() {
        System.out.println("Team [" + teamName + "] starting match with players:");
        players.forEach(p -> System.out.println("  - " + p.getName()));
    }

    public int size() { return players.size(); }
}
