package ludo.core.entities;

import ludo.core.utils.GameUtils;
import java.util.*;
import java.util.stream.Collectors;

public class BotPlayer extends Player {//TODO check usage not used currently
    private static final int CAPTURE_SCORE = 100;
    private static final int SAFETY_SCORE = 50;
    private static final int PROGRESS_SCORE = 30;
    private static final int HOME_STRETCH_SCORE = 80;
    private static final int BLOCKING_SCORE = 40;

    public BotPlayer(String name, String color) {
        super(name, color);
    }

    public int chooseBestMove(Board board, int diceRoll, List<Player> allPlayers) {
        List<PawnMove> possibleMoves = generatePossibleMoves(board, diceRoll);

        if (possibleMoves.isEmpty()) {
            return -1; // No valid moves
        }

        // Evaluate each move and choose the best one
        PawnMove bestMove = possibleMoves.stream()
            .max(Comparator.comparingInt(move ->
                evaluateMove(move, board, allPlayers)))
            .orElse(possibleMoves.get(0));

        return bestMove.pawnIndex;
    }

    private List<PawnMove> generatePossibleMoves(Board board, int diceRoll) {
        List<PawnMove> moves = new ArrayList<>();

        for (int i = 0; i < getPawns().size(); i++) {
            Pawn pawn = getPawns().get(i);

            // Check if move is valid
            if (canMovePawn(pawn, board, diceRoll)) {
                int newPosition = calculateNewPosition(pawn.getPosition(), diceRoll, board);
                moves.add(new PawnMove(i, pawn.getPosition(), newPosition));
            }
        }

        return moves;
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

    private int evaluateMove(PawnMove move, Board board, List<Player> allPlayers) {
        int score = 0;

        // Base progress score
        score += evaluateProgress(move, board);

        // Check for capture opportunity
        score += evaluateCapture(move, allPlayers);

        // Evaluate safety
        score += evaluateSafety(move, board);

        // Consider home stretch
        score += evaluateHomeStretch(move, board);

        // Strategic blocking
        score += evaluateBlocking(move, allPlayers, board);

        return score;
    }

    private int evaluateProgress(PawnMove move, Board board) {
        // Higher score for moves that make more progress toward home
        int progressScore = 0;

        // Starting move bonus
        if (move.startPosition == -1) {
            progressScore += 20;
        }

        // Progress toward home
        int distanceToHome = calculateDistanceToHome(move.newPosition, board);
        progressScore += (board.getTotalSpaces() - distanceToHome) * PROGRESS_SCORE / board.getTotalSpaces();

        return progressScore;
    }

    private int evaluateCapture(PawnMove move, List<Player> allPlayers) {
        // Check if move would capture an opponent's pawn
        for (Player player : allPlayers) {
            if (player != this) {
                for (Pawn opponentPawn : player.getPawns()) {
                    if (opponentPawn.getPosition() == move.newPosition) {
                        return CAPTURE_SCORE;
                    }
                }
            }
        }
        return 0;
    }

    private int evaluateSafety(PawnMove move, Board board) {
        // Higher score for moves to safe spots
        if (board.isSafeSpot(move.newPosition)) {
            return SAFETY_SCORE;
        }
        return 0;
    }

    private int evaluateHomeStretch(PawnMove move, Board board) {
        // Bonus for moves that enter or progress in home stretch
        if (board.isHomeColumn(move.newPosition, getColor())) {
            return HOME_STRETCH_SCORE;
        }
        return 0;
    }

    private int evaluateBlocking(PawnMove move, List<Player> allPlayers, Board board) {
        // Score blocking moves that impede opponents
        int blockingScore = 0;

        for (Player player : allPlayers) {
            if (player != this) {
                for (Pawn opponentPawn : player.getPawns()) {
                    if (isBlockingPosition(move.newPosition, opponentPawn.getPosition(), board)) {
                        blockingScore += BLOCKING_SCORE;
                    }
                }
            }
        }

        return blockingScore;
    }

    private boolean isBlockingPosition(int position, int opponentPosition, Board board) {
        // Check if our position blocks opponent's optimal path
        int opponentDistance = calculateDistanceToHome(opponentPosition, board);
        int throughPosition = calculateDistanceToHome(position, board);
        return throughPosition < opponentDistance;
    }

    private int calculateDistanceToHome(int position, Board board) {
        int startPos = board.getStartPosition(getColor());
        int homeEntry = (startPos + board.getTotalSpaces() - 1) % board.getTotalSpaces();

        if (position >= board.getTotalSpaces()) {
            // Already in home column
            return board.getHomeColumnSize() - (position - board.getTotalSpaces());
        }

        // Calculate distance around the board
        int distance = (homeEntry - position + board.getTotalSpaces()) % board.getTotalSpaces();
        return distance + board.getHomeColumnSize();
    }

    private static class PawnMove {
        final int pawnIndex;
        final int startPosition;
        final int newPosition;

        PawnMove(int pawnIndex, int startPosition, int newPosition) {
            this.pawnIndex = pawnIndex;
            this.startPosition = startPosition;
            this.newPosition = newPosition;
        }
    }

    private int calculateNewPosition(int currentPosition, int steps, Board board) {
        return GameUtils.calculateNextPosition(currentPosition, steps);
    }
}
