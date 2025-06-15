package ludo.core.validation;

import ludo.core.entities.Board;
import ludo.core.entities.Pawn;
import ludo.core.entities.Player;
import ludo.core.utils.Constants;

import java.util.logging.Logger;

public class MoveValidator {
    private static final Logger LOGGER = Logger.getLogger(MoveValidator.class.getName());

    private static int getHomeColumnStartForColor(String color) {
        switch (color.toUpperCase()) {
            case "YELLOW": return 52;
            case "BLUE":   return 58;
            case "RED":    return 64;
            case "GREEN":  return 70;
            default:       return -1;
        }
    }
    /**
     * Validates if a move is legal according to Ludo rules
     */
    public static boolean isValidMove(Player player, Pawn pawn, int steps, Board board) {
        if (pawn.isFinished()) {
            return false;
        }
        if (pawn.isHome()) {
            return steps == 6;
        }

        int currentPosition = pawn.getPosition();

        // --- This block is for pawns already in the home column ---
        if (currentPosition >= 52) {
            int homeColumnStart;
            switch (player.getColor().toUpperCase()) {
                case "YELLOW": homeColumnStart = 52; break;
                case "BLUE":   homeColumnStart = 58; break;
                case "RED":    homeColumnStart = 64; break;
                case "GREEN":  homeColumnStart = 70; break;
                default:       return false;
            }
            int targetBasePosition = homeColumnStart + Constants.HOME_COLUMN_SIZE;
            return (currentPosition + steps) <= targetBasePosition;
        }

        // --- This block is for pawns on the main track ---
        int startPos = board.getStartPositionIndex(player.getColor());
        int homeEntryPos = (startPos - 1 + 52) % 52;
        int distToEntry = (homeEntryPos - currentPosition + 52) % 52;

        if (steps >= distToEntry) {
            int stepsIntoHome = steps - distToEntry;
            return stepsIntoHome <= (Constants.HOME_COLUMN_SIZE + 1);
        }

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
