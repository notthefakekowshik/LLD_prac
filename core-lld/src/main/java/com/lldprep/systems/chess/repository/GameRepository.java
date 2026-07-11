package com.lldprep.systems.chess.repository;

import com.lldprep.systems.chess.model.Game;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameRepository {
    private final Map<String, Game> games = new ConcurrentHashMap<>();

    public void save(Game game) {
        games.put(game.getId(), game);
    }

    public Game getById(String gameId) {
        return games.get(gameId);
    }

    public int count() {
        return games.size();
    }
}
