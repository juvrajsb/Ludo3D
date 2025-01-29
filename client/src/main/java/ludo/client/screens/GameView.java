package ludo.client.screens;

import ludo.core.entities.Player;
import java.util.List;

/**
 * Interface defining all required methods for game view components.
 * This ensures consistency between different screen implementations.
 */
public interface GameView {
    // Game state updates
    void updateGameState(String state);
    void setCurrentPlayer(String playerColor);
    Player getCurrentPlayer();

    // Pawn management
    void updatePawnPosition(int pawnIndex, int newPosition);
    void updatePlayerPawns(String color, List<Integer> positions);
    void enablePawnSelection();
    void playMoveAnimation(int pawnIndex, int newPosition);

    // Player management
    void addPlayer(Player player);
    void removePlayer(String playerName);

    // Dice handling
    void updateDiceDisplay(int value);

    // UI controls
    void enableControls();
    void disableControls();
    void showMessage(String message);
    void showError(String message);
    void showWinnerScreen(String winner);

    // Waiting room
    void showWaitingRoom();
    void updateWaitingRoom();
}
