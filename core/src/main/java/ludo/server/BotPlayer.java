package ludo.server;

import ludo.Pawn;

import java.util.ArrayList;
import java.util.Comparator;
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
        if (pawn.isHome()) {
            return diceRoll == 6;  // Can only leave home with a 6
        }

        int currentPosition = pawn.getPosition();
        int playerStart = board.getStartPosition(getColor());
        int newPosition = currentPosition + diceRoll;
        int entryPoint = (playerStart - 1 + board.getTotalSpaces()) % board.getTotalSpaces();

        // Check if pawn will enter home column
        if (currentPosition <= entryPoint && newPosition > entryPoint) {
            int stepsIntoHome = newPosition - entryPoint - 1;
            if (stepsIntoHome >= board.getHomeColumnSize()) {
                return false;  // Would overshoot home
            }

            // Check if home column position is blocked by own pawn
            int homePosition = board.getTotalSpaces() + stepsIntoHome;
            for (Pawn otherPawn : getPawns()) {
                if (otherPawn != pawn && otherPawn.getPosition() == homePosition) {
                    return false;
                }
            }
            return true;
        }

        // Normal board movement
        newPosition = newPosition % board.getTotalSpaces();

        // Check if blocked by own pawn on non-safe spot
        for (Pawn otherPawn : getPawns()) {
            if (otherPawn != pawn && otherPawn.getPosition() == newPosition
                && !board.isSafeSpot(newPosition)) {
                return false;
            }
        }

        return true;
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
        if (pawn.isHome() && diceRoll == 6) {
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
        return false;
    }

    private int getPawnIndex(Pawn pawn) {
        return getPawns().indexOf(pawn);
    }
}
