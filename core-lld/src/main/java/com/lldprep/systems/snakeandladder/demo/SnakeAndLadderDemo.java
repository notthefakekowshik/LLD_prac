package com.lldprep.systems.snakeandladder.demo;

import com.lldprep.systems.snakeandladder.exception.InvalidBoardException;
import com.lldprep.systems.snakeandladder.exception.InvalidGameStateException;
import com.lldprep.systems.snakeandladder.factory.BoardFactory;
import com.lldprep.systems.snakeandladder.model.Board;
import com.lldprep.systems.snakeandladder.model.Jump;
import com.lldprep.systems.snakeandladder.model.Ladder;
import com.lldprep.systems.snakeandladder.model.Player;
import com.lldprep.systems.snakeandladder.model.Snake;
import com.lldprep.systems.snakeandladder.model.TurnResult;
import com.lldprep.systems.snakeandladder.policy.BounceBackMovePolicy;
import com.lldprep.systems.snakeandladder.policy.Die;
import com.lldprep.systems.snakeandladder.policy.ExactLandingMovePolicy;
import com.lldprep.systems.snakeandladder.policy.ExtraTurnOnMaxRollPolicy;
import com.lldprep.systems.snakeandladder.policy.MovePolicy;
import com.lldprep.systems.snakeandladder.policy.SimpleTurnPolicy;
import com.lldprep.systems.snakeandladder.policy.StandardDie;
import com.lldprep.systems.snakeandladder.service.SnakeAndLadderGame;

import java.util.List;
import java.util.Random;

/**
 * Exercises every functional requirement. Seeded die → reproducible playthrough.
 */
public class SnakeAndLadderDemo {

    public static void main(String[] args) {
        System.out.println("=== Snake & Ladder Demo ===\n");
        turnByTurnPlaythrough();
        bonusAndForfeitRules();
        overshootPolicies();
        boardValidation();
        movesAfterGameOver();
    }

    // FR1-FR8: configured board, 2 players, dice, jumps, exact-landing win, turn rotation.
    // Uses ExtraTurnOnMaxRollPolicy so a 6 grants an extra turn (same player rolls again).
    private static void turnByTurnPlaythrough() {
        System.out.println("--- Playthrough on a compact 25-cell board (seeded die, six = extra turn) ---");
        List<Jump> jumps = List.of(
            new Ladder(3, 19), new Ladder(7, 21), new Ladder(11, 23),
            new Snake(18, 5),  new Snake(24, 8)
        );
        Board board = BoardFactory.custom(25, jumps);
        System.out.println("Jumps: " + board.getJumps() + "\n");

        SnakeAndLadderGame game = new SnakeAndLadderGame(
            board,
            new StandardDie(new Random(11)),
            new ExactLandingMovePolicy(),
            new
                ExtraTurnOnMaxRollPolicy(),
            List.of(new Player("p1", "Alice"), new Player("p2", "Bob")));

        while (!game.isFinished()) {
            TurnResult t = game.playTurn();
            String note = t.tookJump() ? " (jumped!)" : (t.stayedPut() ? " (overshoot — stays)" : "");
            System.out.printf("%-5s rolled %d : %2d -> %2d%s%n",
                t.playerName(), t.diceRoll(), t.fromPosition(), t.toPosition(), note);
            if (t.diceRoll() == 6) {
                System.out.printf("      %s rolled a 6 — extra turn!%n", t.playerName());
            }
        }
        System.out.println("\nWinner: " + game.getWinner().orElseThrow().getName() + "\n");
    }

    // Curveball: the turn rule is a pluggable strategy. Scripted die forces the exact sequence that
    // shows a 6 → extra turn, and three 6s in a row → forfeit (no move, turn passes).
    private static void bonusAndForfeitRules() {
        System.out.println("--- Six-rule via ExtraTurnOnMaxRollPolicy (scripted die: 6,6,2 | 6,6,6 | 3) ---");
        Die die = new ScriptedDie(6, 6, 6, 2, 6, 6, 6, 3);
        Board board = BoardFactory.custom(60, List.of()); // no jumps — keep the focus on turns
        SnakeAndLadderGame game = new SnakeAndLadderGame(
            board, die, new ExactLandingMovePolicy(), new ExtraTurnOnMaxRollPolicy(),
            List.of(new Player("p1", "Alice"), new Player("p2", "Bob")));

        for (int i = 0; i < 7; i++) {
            TurnResult t = game.playTurn();
            String note;
            if (t.diceRoll() == die.maxValue() && t.stayedPut()) {
                note = " (third six — FORFEIT, turn passes)";
            } else if (t.diceRoll() == die.maxValue()) {
                note = " (six — extra turn!)";
            } else {
                note = " (passes turn)";
            }
            System.out.printf("%-5s rolled %d : %2d -> %2d%s%n",
                t.playerName(), t.diceRoll(), t.fromPosition(), t.toPosition(), note);
        }
        System.out.println();
    }

    // Curveball: swap the overshoot rule with zero game-loop changes.
    private static void overshootPolicies() {
        System.out.println("--- Overshoot rule is pluggable (at cell 23 on a 25-board, roll 5) ---");
        MovePolicy exact = new ExactLandingMovePolicy();
        MovePolicy bounce = new BounceBackMovePolicy();
        System.out.println("ExactLanding : lands on " + exact.computeTarget(23, 5, 25) + " (stays — no exact finish)");
        System.out.println("BounceBack   : lands on " + bounce.computeTarget(23, 5, 25) + " (reflects the overshoot)\n");
    }

    // FR9: board setup is validated at construction.
    private static void boardValidation() {
        System.out.println("--- Board validation rejects bad setups ---");
        tryBuild("Snake going up", () -> new Snake(5, 10));
        tryBuild("Ladder going down", () -> new Ladder(20, 4));
        tryBuild("Out-of-bounds jump", () -> BoardFactory.custom(20, List.of(new Ladder(3, 99))));
        tryBuild("Two jumps same start", () ->
            BoardFactory.custom(30, List.of(new Ladder(3, 19), new Snake(3, 1))));
        tryBuild("Chained jumps", () ->
            BoardFactory.custom(30, List.of(new Ladder(3, 19), new Snake(19, 2))));
        System.out.println();
    }

    private static void movesAfterGameOver() {
        System.out.println("--- Moves after game over are rejected ---");
        Board board = BoardFactory.custom(15, List.of(new Ladder(2, 14)));
        SnakeAndLadderGame game = new SnakeAndLadderGame(
            board, new StandardDie(new Random(3)), new ExactLandingMovePolicy(), new SimpleTurnPolicy(),
            List.of(new Player("p1", "Alice"), new Player("p2", "Bob")));
        game.play();
        System.out.println("Winner: " + game.getWinner().orElseThrow().getName());
        try {
            game.playTurn();
        } catch (InvalidGameStateException e) {
            System.out.println("Caught: " + e.getMessage());
        }
    }

    private static void tryBuild(String label, Runnable build) {
        try {
            build.run();
            System.out.println(label + ": (no error — unexpected)");
        } catch (InvalidBoardException e) {
            System.out.println(label + ": rejected — " + e.getMessage());
        }
    }

    /** A deterministic die that replays a fixed sequence — for demoing exact turn scenarios. */
    private static final class ScriptedDie implements Die {
        private final int max;
        private final int[] rolls;
        private int index = 0;

        ScriptedDie(int max, int... rolls) {
            this.max = max;
            this.rolls = rolls;
        }

        @Override
        public int roll() {
            return rolls[index++];
        }

        @Override
        public int maxValue() {
            return max;
        }
    }
}
