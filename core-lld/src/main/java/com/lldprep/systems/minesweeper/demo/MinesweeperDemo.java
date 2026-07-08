package com.lldprep.systems.minesweeper.demo;

import com.lldprep.systems.minesweeper.exception.GameOverException;
import com.lldprep.systems.minesweeper.exception.OutOfBoundsException;
import com.lldprep.systems.minesweeper.factory.BoardFactory;
import com.lldprep.systems.minesweeper.model.Board;
import com.lldprep.systems.minesweeper.model.Cell;
import com.lldprep.systems.minesweeper.model.Difficulty;
import com.lldprep.systems.minesweeper.model.GameStatus;
import com.lldprep.systems.minesweeper.model.MoveResult;
import com.lldprep.systems.minesweeper.policy.RandomMinePlacementStrategy;
import com.lldprep.systems.minesweeper.policy.SafeFirstClickMinePlacementStrategy;
import com.lldprep.systems.minesweeper.service.MinesweeperGame;

import java.util.Random;

/**
 * Exercises every functional requirement. Uses seeded RNG + post-placement board inspection so
 * runs are reproducible and the win/loss paths are deterministic regardless of layout.
 */
public class MinesweeperDemo {

    public static void main(String[] args) {
        System.out.println("=== Minesweeper Demo ===\n");

        floodFillAndFlagging();
//        lossScenario();
//        winScenario();
//        curveballFirstClickSafe();
//        errorHandling();
    }

    // FR2/FR4/FR5/FR6 — reveal, flood-fill, flag/unflag, cannot-reveal-flagged.
    private static void floodFillAndFlagging() {
        System.out.println("--- FR2/FR4: Reveal + flood-fill (5x5, 3 mines, first-click-safe) ---");
        Board board = BoardFactory.custom(5, 5, 3, new SafeFirstClickMinePlacementStrategy(new Random(7)));
        MinesweeperGame game = new MinesweeperGame(board);

        System.out.println("Before first reveal:\n" + board.render());
        MoveResult r = game.reveal(0, 0);
        System.out.println(r.message());
        System.out.println("After revealing (0,0) — flood-fill cascades:\n" + board.render());

        System.out.println("--- FR5/FR6: Flag a cell, then confirm it cannot be revealed ---");
        int[] hidden = firstHiddenNonMine(board);
        game.toggleFlag(hidden[0], hidden[1]);
        System.out.println("Flagged (" + hidden[0] + "," + hidden[1] + "):\n" + board.render());
        game.reveal(hidden[0], hidden[1]); // no-op — flagged cells are protected
        boolean stillHidden = !board.getCell(hidden[0], hidden[1]).isRevealed();
        System.out.println("Reveal on flagged cell was a no-op? " + stillHidden);
        game.toggleFlag(hidden[0], hidden[1]); // unflag
        System.out.println("Unflagged — flag removed.\n");
    }

    // FR3: revealing a mine loses the game.
    private static void lossScenario() {
        System.out.println("--- FR3: Hit a mine → LOST ---");
        Board board = BoardFactory.custom(5, 5, 3, new SafeFirstClickMinePlacementStrategy(new Random(7)));
        MinesweeperGame game = new MinesweeperGame(board);
        game.reveal(0, 0); // places mines safely away from (0,0)

        int[] mine = firstMine(board);
        MoveResult r = game.reveal(mine[0], mine[1]);
        System.out.println(r.message());
        System.out.println("Status: " + game.getStatus());
        System.out.println("Final board (all mines shown):\n" + board.render());
    }

    // FR7: revealing all non-mine cells wins.
    private static void winScenario() {
        System.out.println("--- FR7: Reveal every safe cell → WON ---");
        Board board = BoardFactory.custom(5, 5, 3, new SafeFirstClickMinePlacementStrategy(new Random(7)));
        MinesweeperGame game = new MinesweeperGame(board);
        game.reveal(0, 0);

        outer:
        for (int row = 0; row < board.getRows(); row++) {
            for (int col = 0; col < board.getCols(); col++) {
                if (game.getStatus() != GameStatus.IN_PROGRESS) break outer;
                if (!board.getCell(row, col).isMine() && board.getCell(row, col).isHidden()) {
                    game.reveal(row, col);
                }
            }
        }
        System.out.println("Status: " + game.getStatus());
        System.out.println("Final board:\n" + board.render());
    }

    // Curveball: swap placement policy — plain random can lose on the very first click.
    private static void curveballFirstClickSafe() {
        System.out.println("--- Curveball: RandomMinePlacementStrategy (first click NOT safe) ---");
        // Dense board so a first-click mine is likely; seed pinned for reproducibility.
        Board board = BoardFactory.custom(3, 3, 8, new RandomMinePlacementStrategy(new Random(1)));
        MinesweeperGame game = new MinesweeperGame(board);
        MoveResult r = game.reveal(1, 1);
        System.out.println("First-click result with plain random: " + r.status() + " — " + r.message());
        System.out.println("→ SafeFirstClickMinePlacementStrategy avoids this with zero board changes.\n");
    }

    // Bounds + post-game move rejection.
    private static void errorHandling() {
        System.out.println("--- Error handling: bounds + moves after game over ---");
        Board board = BoardFactory.fromDifficulty(Difficulty.BEGINNER,
            new SafeFirstClickMinePlacementStrategy(new Random(42)));
        MinesweeperGame game = new MinesweeperGame(board);

        try {
            game.reveal(99, 99);
        } catch (OutOfBoundsException e) {
            System.out.println("Caught: " + e.getMessage());
        }

        game.reveal(0, 0);
        int[] mine = firstMine(board);
        game.reveal(mine[0], mine[1]); // lose
        try {
            game.reveal(1, 1);
        } catch (GameOverException e) {
            System.out.println("Caught: " + e.getMessage());
        }
    }

    private static int[] firstMine(Board board) {
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (board.getCell(r, c).isMine()) return new int[]{r, c};
            }
        }
        throw new IllegalStateException("no mine found");
    }

    private static int[] firstHiddenNonMine(Board board) {
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                Cell cell = board.getCell(r, c);
                if (cell.isHidden() && !cell.isMine()) return new int[]{r, c};
            }
        }
        throw new IllegalStateException("no hidden safe cell found");
    }
}
