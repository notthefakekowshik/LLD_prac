package com.lldprep.systems.chess.demo;

import com.lldprep.systems.chess.exception.ChessException;
import com.lldprep.systems.chess.model.Game;
import com.lldprep.systems.chess.model.Move;
import com.lldprep.systems.chess.model.enums.GameState;
import com.lldprep.systems.chess.service.ChessFacade;

public class ChessDemo {

    public static void main(String[] args) {
        printHeader();

        scholarsMate();        // 1
        castling();            // 2
        enPassant();           // 3
        pawnPromotion();       // 4
        stalemate();           // 5
        validationFailures();  // 6
        moveHistory();          // 7
        successSummary();       // 8
    }

    private static ChessFacade facade = new ChessFacade();

    // 1. Scholar's Mate — the classic 4-move checkmate.
    private static void scholarsMate() {
        scenario("1. Scholar's Mate — White checkmates in 4 moves");
        Game game = facade.createGame("Morphy", "Amateur");
        printBoard(game);

        String gid = game.getId();
        mw(gid, "e2", "e4");
        mb(gid, "e7", "e5");
        System.out.println("  1. e4 e5 — both open with king's pawn");

        mw(gid, "d1", "h5");
        mb(gid, "b8", "c6");
        System.out.println("  2. Qh5 Nc6 — White brings queen out, Black defends");

        mw(gid, "f1", "c4");
        mb(gid, "g8", "f6");
        System.out.println("  3. Bc4 Nf6 — White targets f7, Black develops");

        mw(gid, "h5", "f7");
        System.out.println("  4. Qxf7# — CHECKMATE!");

        assertState(game, GameState.WHITE_WINS, "Scholar's Mate — White should win");
        printBoard(game);
        System.out.println();
    }

    // 2. Castling demonstration.
    private static void castling() {
        scenario("2. Castling (kingside + queenside)");

        // Kingside O-O
        {
            Game game = facade.createGame("Fischer", "Spassky");
            String gid = game.getId();
            mw(gid, "e2", "e4"); mb(gid, "e7", "e5");
            mw(gid, "g1", "f3"); mb(gid, "b8", "c6");
            mw(gid, "f1", "c4"); mb(gid, "f8", "c5");
            mw(gid, "e1", "g1");
            System.out.println("  4. O-O — White castles kingside (Kg1, Rf1)");
            assertState(game, GameState.BLACK_TO_MOVE, "Kingside castling: should be Black's turn");
            printBoard(game);
            System.out.println();
        }

        // Queenside O-O-O for both
        {
            Game game = facade.createGame("Kasparov", "Karpov");
            String gid = game.getId();
            mw(gid, "d2", "d4"); mb(gid, "d7", "d5");
            mw(gid, "b1", "c3"); mb(gid, "b8", "c6");
            mw(gid, "c1", "f4"); mb(gid, "c8", "f5");
            mw(gid, "d1", "d3"); mb(gid, "d8", "d6");
            mw(gid, "e1", "c1"); mb(gid, "e8", "c8");
            System.out.println("  Both sides castled queenside — White Kc1, Black Kc8");
            printBoard(game);
            System.out.println();
        }
    }

    // 3. En passant capture.
    private static void enPassant() {
        scenario("3. En passant capture");
        Game game = facade.createGame("Alpha", "Beta");
        String gid = game.getId();

        mw(gid, "e2", "e4"); mb(gid, "d7", "d5");
        mw(gid, "e4", "e5");
        mb(gid, "f7", "f5");
        System.out.println("  2... f5 — Black pawn double-steps next to e5");
        mw(gid, "e5", "f6");
        System.out.println("  3. exf6 e.p. — White captures en passant!");

        assertState(game, GameState.BLACK_TO_MOVE, "En passant executed, should be Black's turn");
        printBoard(game);
        System.out.println("White pawn now on f6; Black f5 pawn removed");
        System.out.println();
    }

    // 4. Pawn promotion.
    private static void pawnPromotion() {
        scenario("4. Pawn promotion");
        Game game = facade.createGame("Promoted", "Fallen");
        String gid = game.getId();

        mw(gid, "a2", "a4"); mb(gid, "b8", "c6");   // Move Black knight away from b8
        mw(gid, "a4", "a5"); mb(gid, "h7", "h6");
        mw(gid, "a5", "a6"); mb(gid, "g7", "g6");
        mw(gid, "a6", "b7"); mb(gid, "f7", "f6");   // White captures b7, now on b7 with b8 cleared
        System.out.println("  4. axb7 — White pawn captures b7");

        mw(gid, "b7", "b8");
        System.out.println("  5. b8=Q — White pawn promotes to Queen (default)!");

        assertState(game, GameState.BLACK_TO_MOVE, "Promotion done, should be Black's turn");
        printBoard(game);
        System.out.println("White Queen on b8 — the promoted pawn");
        System.out.println();
    }

    // 5. Stalemate verification — explain the mechanic.
    private static void stalemate() {
        scenario("5. Stalemate detection mechanic");
        System.out.println("Stalemate = player to move has zero legal moves AND is not in check.");
        System.out.println("\nExample position: White Ka1, Black Kc3, Black Qb2");
        System.out.println("  White to move: squares b1,b2,a2 all attacked; a1 king has nowhere to go.");
        System.out.println("  Not in check (no piece directly attacks a1) → STALEMATE.");
        System.out.println("\nImplementation:");
        System.out.println("  Board.isStalemate(color) = !isCheck(color) && hasNoLegalMoves(color)");
        System.out.println("  hasNoLegalMoves scans all friendly pieces; any piece with ≥1 valid move → false.");
        System.out.println("  getValidMoves() = getCandidateSquares() filtered through copy-board self-check test.");
        System.out.println();
    }

    // 6. Validation failures.
    private static void validationFailures() {
        scenario("6. Expected validation failures");
        Game game = facade.createGame("Tester", "Validator");
        String gid = game.getId();

        expectFailure("Black tries to move first (e7→e5)", () ->
            facade.makeMove(gid, "e7", "e5"));

        mw(gid, "e2", "e4");   // White: 1.e4 — Black's turn
        mb(gid, "e7", "e5");   // Black: 1...e5 — White's turn

        // White's turn — test basic geometry violations
        expectFailure("Rook blocked by own pawn (Ra1→a3)", () ->
            facade.makeMove(gid, "a1", "a3"));

        expectFailure("Move from empty square (h4→h5)", () ->
            facade.makeMove(gid, "h4", "h5"));

        expectFailure("Pawn moves backward (e4→e3)", () ->
            facade.makeMove(gid, "e4", "e3"));

        // Set up an absolute pin: Nc3 is pinned by Bb4 to Ke1 on the diagonal.
        mw(gid, "g1", "f3");   // White: 2.Nf3
        mb(gid, "b8", "c6");   // Black: 2...Nc6
        mw(gid, "d2", "d4");   // White: 3.d4
        mb(gid, "e5", "d4");   // Black: 3...exd4
        mw(gid, "f3", "d4");   // White: 4.Nxd4
        mb(gid, "g8", "f6");   // Black: 4...Nf6
        mw(gid, "b1", "c3");   // White: 5.Nc3
        mb(gid, "f8", "b4");   // Black: 5...Bb4 — pins Nc3 to Ke1

        expectFailure("Pinned knight cannot move (Nc3→d5 exposes Ke1 to Bb4)", () ->
            facade.makeMove(gid, "c3", "d5"));

        System.out.println();
    }

    // 7. Move history — read directly from Game.getMoves().
    private static void moveHistory() {
        scenario("7. Move history");
        Game game = facade.createGame("Capablanca", "Alekhine");
        String gid = game.getId();

        mw(gid, "e2", "e4"); mb(gid, "e7", "e5");
        mw(gid, "g1", "f3"); mb(gid, "b8", "c6");
        mw(gid, "f1", "c4");

        System.out.println("Move history for " + game.getId() + ":");
        for (Move move : game.getMoves()) {
            System.out.println("  " + move.notation());
        }
        System.out.println();
    }

    // 8. Final totals.
    private static void successSummary() {
        scenario("8. Success summary");
        System.out.println("Total games created: " + facade.gameCount());
        System.out.println("\nALL CHESS SCENARIOS COMPLETED");
    }

    // --- helpers ---

    private static void mw(String gameId, String from, String to) {
        facade.makeMove(gameId, from, to);
    }

    private static void mb(String gameId, String from, String to) {
        facade.makeMove(gameId, from, to);
    }

    private static void printBoard(Game game) {
        System.out.println(facade.getBoard(game.getId()));
        System.out.println("State: " + game.getState());
    }

    private static void assertState(Game game, GameState expected, String label) {
        GameState actual = game.getState();
        String marker = actual == expected ? "PASS" : "FAIL";
        System.out.println("  [" + marker + "] " + label + " (state=" + actual + ")");
    }

    private static void expectFailure(String label, Runnable action) {
        try {
            action.run();
            System.out.println("  FAIL: expected error — " + label);
        } catch (ChessException | IllegalArgumentException ex) {
            System.out.println("  OK: " + label + " — " + ex.getMessage());
        }
    }

    private static void scenario(String title) {
        System.out.println("------------------------------------------------------------");
        System.out.println(title);
        System.out.println("------------------------------------------------------------");
    }

    private static void printHeader() {
        System.out.println("============================================================");
        System.out.println("CHESS LLD DEMO");
        System.out.println("Patterns: Template Method | Factory | Facade");
        System.out.println("============================================================\n");
    }
}
