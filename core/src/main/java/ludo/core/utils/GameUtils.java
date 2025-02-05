package ludo.core.utils;

import ludo.core.entities.Board;

public class GameUtils {
    /**
     * Calculates the next position on the board given current position and dice roll
     * @param currentPosition Current position on the board
     * @param diceRoll Number rolled on the dice
     * @return The next position after moving diceRoll spaces
     */
    public static int calculateNextPosition(int currentPosition, int diceRoll) {
        if (currentPosition >= Constants.BOARD_SIZE) {
            // Handle home column movement
            return currentPosition + diceRoll;
        }
        return (currentPosition + diceRoll) % Constants.BOARD_SIZE;
    }

    /**
     * Checks if a move would land exactly in a player's home
     * @param currentPosition Current position on the board
     * @param diceRoll Number rolled on the dice
     * @param playerColor Color of the player
     * @param board The game board
     * @return true if the move would land exactly in home, false otherwise
     */
    public static boolean isExactHomeMove(int currentPosition, int diceRoll, String playerColor, Board board) { //TODO check usage not used currently
        int nextPosition = calculateNextPosition(currentPosition, diceRoll);
        int homeColumnBase = Constants.BOARD_SIZE + (board.getStartPosition(playerColor) / 13) * Constants.HOME_COLUMN_SIZE;
        return nextPosition == homeColumnBase + Constants.HOME_COLUMN_SIZE - 1;
    }
}
