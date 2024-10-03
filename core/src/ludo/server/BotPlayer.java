package src.ludo.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class BotPlayer extends Player {
    public BotPlayer(String name, String color) {
        super(name, color);
    }

    public String makeMove(Board board, int diceRoll) {
        List<Pawn> movablePawns = getMovablePawns(board, diceRoll);
        
        if (movablePawns.isEmpty()) {
            return "SKIP";
        }

        Pawn bestPawn = chooseBestPawn(movablePawns, board, diceRoll);
        int pawnIndex = getPawnIndex(bestPawn);
        
        return "MOVE " + pawnIndex;
    }

    private List<Pawn> getMovablePawns(Board board, int diceRoll) {
        return getPawns().stream()
                .filter(pawn -> canMovePawn(pawn, board, diceRoll))
                .collect(Collectors.toList());
    }

    private boolean canMovePawn(Pawn pawn, Board board, int diceRoll) {
        // Implement logic to check if the pawn can move based on the game rules
        // This should consider if the pawn is in the home, on the board, or in the final stretch
    }

    private Pawn chooseBestPawn(List<Pawn> movablePawns, Board board, int diceRoll) {
        return movablePawns.stream()
                .max(Comparator.comparingInt(pawn -> evaluateMove(pawn, board, diceRoll)))
                .orElse(movablePawns.get(0));
    }

    private int evaluateMove(Pawn pawn, Board board, int diceRoll) {
        int score = 0;
        int newPosition = (pawn.getPosition() + diceRoll) % board.getTotalSpaces();

        // Prioritize moving pawns out of home
        if (pawn.isInHome() && diceRoll == 6) {
            score += 100;
        }

        // Prioritize moving pawns to safe spots
        if (board.isSafeSpot(newPosition)) {
            score += 50;
        }

        // Prioritize capturing opponent pawns
        if (canCapturePawn(board, newPosition)) {
            score += 75;
        }

        // Prioritize advancing pawns closer to the finish
        score += newPosition;

        // Avoid moving pawns that are already in good positions
        if (board.isSafeSpot(pawn.getPosition())) {
            score -= 25;
        }

        return score;
    }

    private boolean canCapturePawn(Board board, int position) {
        // Implement logic to check if moving to this position would capture an opponent's pawn
    }

    private int getPawnIndex(Pawn pawn) {
        return getPawns().indexOf(pawn);
    }
}