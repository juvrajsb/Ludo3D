package ludo.client.handlers;

import ludo.client.screens.GameScreen;
import ludo.core.entities.Player;

// Client-side player join handler
public class ClientPlayerJoinHandler {
    private final GameScreen gameScreen;

    public ClientPlayerJoinHandler(GameScreen gameScreen) {
        this.gameScreen = gameScreen;
    }

    public void handlePlayerJoined(Player player) {
        gameScreen.addPlayer(player);
        gameScreen.updateWaitingRoom();
    }

    public void handlePlayerLeft(String playerName) {
        gameScreen.removePlayer(playerName);
        gameScreen.updateWaitingRoom();
    }

    public void handleJoinResponse(boolean accepted, String message) {
        if (accepted) {
            gameScreen.showWaitingRoom();
        } else {
            gameScreen.showError(message);
        }
    }
}
