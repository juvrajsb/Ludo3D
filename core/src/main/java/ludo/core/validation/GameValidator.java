package ludo.core.validation;

import ludo.core.entities.Player;

public class GameValidator {
    public static boolean isValidTurn(Player currentPlayer, String connectionId) {
        return currentPlayer != null &&
               currentPlayer.getName().equals(connectionId);
    }

    public static boolean isValidGameState(boolean gameStarted, int playerCount) {
        return gameStarted && playerCount >= 2;
    }

    public static boolean isValidPlayerCount(int currentCount, int maxPlayers) {
        return currentCount < maxPlayers;
    }
}
