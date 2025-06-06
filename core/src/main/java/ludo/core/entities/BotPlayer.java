package ludo.core.entities;

import ludo.core.utils.Constants;
import java.util.*;
import java.util.logging.Logger;

public class BotPlayer extends Player {
    private static final Logger LOGGER = Logger.getLogger(BotPlayer.class.getName());

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
            LOGGER.info("No valid moves available for " + getColor());
            return -1; // No valid moves
        }

        PawnMove bestMove = null;
        int bestScore = Integer.MIN_VALUE;

        for (PawnMove move : possibleMoves) {
            int score = evaluateMove(move, board, allPlayers);
            LOGGER.info("Evaluating move: Pawn " + move.pawnIndex +
                " to position " + move.newPosition + " - Score: " + score);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }

        if (bestMove != null) {
            LOGGER.info("Best move selected: Pawn " + bestMove.pawnIndex +
                " from " + bestMove.startPosition + " to " + bestMove.newPosition +
                " with score " + bestScore);
            return bestMove.pawnIndex;
        } else {
            LOGGER.warning("Failed to select a best move despite having possible moves");
            return possibleMoves.get(0).pawnIndex; // Fallback to first move
        }
    }

    private List<PawnMove> generatePossibleMoves(Board board, int diceRoll) {
        List<PawnMove> moves = new ArrayList<>();

        for (int i = 0; i < getPawns().size(); i++) {
            Pawn pawn = getPawns().get(i);

            // Check if move is valid
            if (canMovePawn(pawn, board, diceRoll)) {
                int newPosition = calculateNewPosition(pawn.getPosition(), diceRoll);
                moves.add(new PawnMove(i, pawn.getPosition(), newPosition));
            }
        }

        return moves;
    }

    private boolean canMovePawn(Pawn pawn, Board board, int diceRoll) {
        if (pawn.isHome()) {
            boolean result = diceRoll == 6;
            LOGGER.fine("Checking if pawn can leave home with roll " + diceRoll + ": " + result);
            return result;
        }

        if (pawn.isFinished()) {
            LOGGER.fine("Pawn is already finished, cannot move");
            return false;
        }

        int currentPosition = pawn.getPosition();
        int playerStart = board.getStartPosition(getColor());
        int newPosition = currentPosition + diceRoll;
        int entryPoint = (playerStart - 1 + board.getTotalSpaces()) % board.getTotalSpaces();

        LOGGER.fine("Calculated new position: current=" + currentPosition +
            ", start=" + playerStart + ", new=" + newPosition);

        // Check if pawn is in home column
        if (board.isHomeColumn(currentPosition, getColor())) {
            // In home column, can move any number of steps as long as we don't overshoot
            int homeStart = board.getTotalSpaces() + 
                (board.getStartPositionIndex(getColor()) / 13) * board.getHomeColumnSize();
            int distanceToEnd = (homeStart + board.getHomeColumnSize() - 1) - currentPosition;
            
            // Log the home column calculation details
            LOGGER.info("Home column check - Current: " + currentPosition + 
                ", Home Start: " + homeStart + 
                ", Distance to End: " + distanceToEnd + 
                ", Roll: " + diceRoll);
            
            // Can move if the roll doesn't overshoot the end
            return diceRoll <= distanceToEnd;
        }

        if (currentPosition <= entryPoint && newPosition > entryPoint) {
            int stepsIntoHome = newPosition - entryPoint - 1;
            if (stepsIntoHome >= board.getHomeColumnSize()) {
                LOGGER.info("Move rejected - Would overshoot home column: " + stepsIntoHome + 
                    " steps into home (max: " + (board.getHomeColumnSize() - 1) + ")");
                return false;  // Would overshoot home
            }

            // Pawns of same color can stack in home column
            return true;
        }

        // Normal board movement
        newPosition = newPosition % board.getTotalSpaces();

        // Pawns of same color can stack on non-safe spots
        for (Pawn otherPawn : getPawns()) {
            if (otherPawn != pawn && otherPawn.getPosition() == newPosition
                && !board.isSafeSpot(newPosition) && !otherPawn.getColor().equals(getColor())) {
                LOGGER.info("Move rejected - Position " + newPosition + " blocked by opponent pawn");
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

        // Bonus for reaching home
        if (board.isHomeColumn(move.newPosition, getColor())) {
            int homeStart = board.getTotalSpaces() + 
                (board.getStartPositionIndex(getColor()) / 13) * board.getHomeColumnSize();
            int distanceToEnd = (homeStart + board.getHomeColumnSize() - 1) - move.newPosition;
            if (distanceToEnd == 0) {
                score += 200; // Big bonus for reaching home
            }
        }

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
        // Only give bonus for moves that get closer to home or enter home column
        if (board.isHomeColumn(move.newPosition, getColor())) {
            int homeStart = board.getTotalSpaces() + 
                (board.getStartPositionIndex(getColor()) / 13) * board.getHomeColumnSize();
            int distanceToEnd = (homeStart + board.getHomeColumnSize() - 1) - move.newPosition;
            return HOME_STRETCH_SCORE * (board.getHomeColumnSize() - distanceToEnd);
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

    private int calculateNewPosition(int currentPosition, int steps) {
        if (currentPosition >= Constants.BOARD_SIZE) {
            return currentPosition + steps;
        }
        return (currentPosition + steps) % Constants.BOARD_SIZE;
    }
}
