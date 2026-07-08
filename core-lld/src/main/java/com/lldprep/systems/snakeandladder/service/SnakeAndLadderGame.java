package com.lldprep.systems.snakeandladder.service;

import com.lldprep.systems.snakeandladder.exception.InvalidGameStateException;
import com.lldprep.systems.snakeandladder.model.Board;
import com.lldprep.systems.snakeandladder.model.GameStatus;
import com.lldprep.systems.snakeandladder.model.Player;
import com.lldprep.systems.snakeandladder.model.TurnResult;
import com.lldprep.systems.snakeandladder.policy.Die;
import com.lldprep.systems.snakeandladder.policy.MovePolicy;
import com.lldprep.systems.snakeandladder.policy.TurnDecision;
import com.lldprep.systems.snakeandladder.policy.TurnPolicy;

import java.util.List;
import java.util.Optional;

/**
 * The game's single entry point — a Facade over the board/die/move-policy subsystem. Callers roll
 * turns and read status without touching dice, jumps, or turn ordering.
 *
 * No interface: there is exactly one game engine and nothing swaps it, so a concrete class IS the
 * facade (Facade is a role, not a Java interface). The real extension seams are the injected
 * strategies — {@link Die}, {@link MovePolicy}, {@link TurnPolicy} — and the board layout, all
 * supplied via the constructor (DIP).
 */
public class SnakeAndLadderGame {

    private static final int MIN_PLAYERS = 2;
    // ponytail: safety net against a pathological die sequence that never lands the win cell under
    // exact-landing; a real die terminates with probability 1. Raise if huge boards ever need it.
    private static final int MAX_TURNS = 100_000;

    private final Board board;
    private final Die die;
    private final MovePolicy movePolicy;
    private final TurnPolicy turnPolicy;
    private final List<Player> players;

    private int currentPlayerIndex = 0;
    // Bonus-roll streak for the current player. Invariant: resets to 0 whenever the turn passes,
    // so a single game-level field is correct — the TurnPolicy returns the next value each roll.
    private int consecutiveBonus = 0;
    private GameStatus status = GameStatus.IN_PROGRESS;
    private Player winner;

    public SnakeAndLadderGame(
            Board board, Die die, MovePolicy movePolicy, TurnPolicy turnPolicy, List<Player> players) {
        if (players == null || players.size() < MIN_PLAYERS) {
            throw new InvalidGameStateException("Need at least " + MIN_PLAYERS + " players");
        }
        this.board = board;
        this.die = die;
        this.movePolicy = movePolicy;
        this.turnPolicy = turnPolicy;
        this.players = List.copyOf(players);
    }

    // CRITICAL SECTION — a game is one turn-based session; a turn (roll + move + jump + win-check)
    // must apply atomically so no two threads interleave on shared position/turn state.
    // ponytail: single lock on the game — right granularity for turn-based play.
    /** Plays the current player's turn, then advances to the next player unless a bonus turn was granted. */
    public synchronized TurnResult playTurn() {
        if (status.isOver()) {
            throw new InvalidGameStateException("Game is already finished — winner: " + winner.getName());
        }

        Player player = players.get(currentPlayerIndex);
        int from = player.getPosition();
        int roll = die.roll();

        TurnDecision decision = turnPolicy.decide(roll, die.maxValue(), consecutiveBonus);
        consecutiveBonus = decision.nextConsecutiveBonus();

        int to = from;
        boolean tookJump = false;
        if (decision.applyMove()) {
            int landing = movePolicy.computeTarget(from, roll, board.getSize());
            to = board.getDestination(landing);            // apply snake/ladder, if any
            tookJump = to != landing;
            player.moveTo(to);

            if (to == board.getSize()) {
                status = GameStatus.FINISHED;
                winner = player;
                return new TurnResult(player.getName(), roll, from, to, tookJump, status);
            }
        }

        // Single place that decides whose turn is next.
        if (!decision.grantsExtraTurn()) {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        }
        return new TurnResult(player.getName(), roll, from, to, tookJump, status);
    }

    /** Runs turns until someone wins; returns the winner. */
    public synchronized Player play() {
        int turns = 0;
        while (!status.isOver()) {
            if (++turns > MAX_TURNS) {
                throw new InvalidGameStateException("Game exceeded " + MAX_TURNS + " turns without a winner");
            }
            playTurn();
        }
        return winner;
    }

    public synchronized boolean isFinished() {
        return status.isOver();
    }

    public synchronized Optional<Player> getWinner() {
        return Optional.ofNullable(winner);
    }

    public synchronized GameStatus getStatus() {
        return status;
    }

    public List<Player> getPlayers() {
        return players; // already immutable
    }
}
