package ludo.core.validation;

import ludo.core.entities.Board;
import ludo.core.utils.Constants;
import ludo.core.entities.Pawn;
import ludo.core.entities.Player;

public class MoveValidator { //TODO check usage not used currently used in an unused method
    public static boolean isValidMove(Player player, Pawn pawn, int steps, Board board) {
        // Basic validation
        if (player == null || pawn == null || steps < 1 || steps > 6) {
            return false;
        }

        // Home validation
        if (pawn.isHome() && steps != 6) {
            return false;
        }

        // Calculate new position
        int newPosition = pawn.getPosition() + steps;

        // Check board boundaries
        if (newPosition >= board.getTotalSpaces()) {
            return false;
        }

        // Check home column entry
        if (board.isHomeColumn(newPosition, player.getColor())) {
            return validateHomeColumnMove(player, newPosition, board);
        }

        // Check if destination is occupied by own pawn
        return !isOccupiedByOwnPawn(player, newPosition % Constants.BOARD_SIZE);
    }

    private static boolean validateHomeColumnMove(Player player, int position, Board board) {
        for (Pawn p : player.getPawns()) {
            if (p.getPosition() == position) {
                return false;
            }
        }
        return true;
    }

    private static boolean isOccupiedByOwnPawn(Player player, int position) {
        return player.getPawns().stream()
            .anyMatch(p -> p.getPosition() == position);
    }
}
