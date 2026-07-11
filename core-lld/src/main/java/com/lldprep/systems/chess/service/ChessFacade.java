package com.lldprep.systems.chess.service;

import com.lldprep.systems.chess.model.Game;
import com.lldprep.systems.chess.model.Move;
import com.lldprep.systems.chess.model.Square;
import com.lldprep.systems.chess.model.enums.PieceType;
import com.lldprep.systems.chess.repository.GameRepository;

// Why: Facade provides a single entry point hiding GameService, GameRepository, and move validation complexity.
public class ChessFacade {
    private final GameRepository gameRepository;
    private final GameService gameService;

    public ChessFacade() {
        this.gameRepository = new GameRepository();
        this.gameService = new GameService(gameRepository);
    }

    public Game createGame(String whiteName, String blackName) {
        return gameService.createGame(whiteName, blackName);
    }

    public Move makeMove(String gameId, String fromAlgebraic, String toAlgebraic) {
        return gameService.makeMove(gameId, fromAlgebraic, toAlgebraic);
    }

    public Move makeMove(String gameId, String fromAlgebraic, String toAlgebraic, PieceType promotion) {
        return gameService.makeMove(gameId, Square.fromAlgebraic(fromAlgebraic), Square.fromAlgebraic(toAlgebraic), promotion);
    }

    public Game getGame(String gameId) {
        return gameService.getGame(gameId);
    }

    public String getBoard(String gameId) {
        Game game = gameService.getGame(gameId);
        return game.getBoard().toString();
    }

    public int gameCount() {
        return gameRepository.count();
    }
}
