package ludo.core.validation;

import ludo.core.entities.Board;
import ludo.core.entities.Pawn;
import ludo.core.entities.Player;

import java.util.logging.Logger;

public class MoveValidator {
    private static final Logger LOGGER = Logger.getLogger(MoveValidator.class.getName());

    /**
     * Validates if a move is legal according to Ludo rules
     */
    public static boolean isValidMove(Player player, Pawn pawn, int steps, Board board) {
        LOGGER.fine(String.format("Validating move - Player: %s, Pawn pos: %d, Steps: %d",
            player.getColor(), pawn.getPosition(), steps));

        // Basic validation
        if (player == null || pawn == null || steps < 1 || steps > 6) {
            LOGGER.info("Invalid move: Basic validation failed");
            return false;
        }

        // Home validation - can only leave home with a 6
        if (pawn.isHome()) {
            if (steps != 6) {
                LOGGER.info("Invalid move: Cannot leave home without a 6");
                return false;
            }
            return true;
        }

        // Finished pawns cannot move
        if (pawn.isFinished()) {
            LOGGER.info("Invalid move: Pawn is already finished");
            return false;
        }

        // Calculate new position
        int currentPosition = pawn.getPosition();
        int playerStartPos = board.getStartPosition(player.getColor());
        int entryPoint = (playerStartPos + board.getTotalSpaces() - 1) % board.getTotalSpaces();
        int newPosition = currentPosition + steps;

        // Handle home column entry
        if (currentPosition < board.getTotalSpaces() && // Not already in home column
            currentPosition <= entryPoint && newPosition > entryPoint) {
            // Entering home column
            LOGGER.fine("Pawn is entering home column");
            int stepsAfterEntry = newPosition - entryPoint - 1;

            // Verify not overshooting home column
            if (stepsAfterEntry >= board.getHomeColumnSize()) {
                LOGGER.info("Invalid move: Would overshoot home column");
                return false;
            }

            // Calculate home column position
            int homeColumnStart = board.getTotalSpaces() +
                (playerStartPos / 13) * board.getHomeColumnSize();
            int homePosition = homeColumnStart + stepsAfterEntry;

            LOGGER.fine("Move into home column valid");
            return true;
        }

        // Handle movement within home column
        if (currentPosition >= board.getTotalSpaces()) {
            // Already in home column
            LOGGER.fine("Pawn is moving within home column");

            // Verify not overshooting target base
            int homeColumnStart = board.getTotalSpaces() +
                (playerStartPos / 13) * board.getHomeColumnSize();
            int targetBasePosition = homeColumnStart + board.getHomeColumnSize();

            if (newPosition > targetBasePosition) {
                LOGGER.info("Invalid move: Would overshoot target base");
                return false;
            }

            LOGGER.fine("Move within home column valid");
            return true;
        }

        // Regular board movement
        int boardPosition = newPosition % board.getTotalSpaces();

        LOGGER.fine("Regular board move valid");
        return true;
    }

    /**
     * Gets a list of valid moves for a player given a dice roll
     */
    public static int[] getValidMoves(Player player, int diceRoll, Board board) {
        if (diceRoll < 1 || diceRoll > 6) {
            return new int[0];
        }

        int[] validPawnIndices = new int[player.getPawns().size()];
        int validCount = 0;

        for (int i = 0; i < player.getPawns().size(); i++) {
            Pawn pawn = player.getPawns().get(i);

            // Skip finished pawns
            if (pawn.isFinished()) continue;

            if (isValidMove(player, pawn, diceRoll, board)) {
                validPawnIndices[validCount++] = i;
            }
        }

        // Create properly sized result array
        int[] result = new int[validCount];
        System.arraycopy(validPawnIndices, 0, result, 0, validCount);

        return result;
    }
}
