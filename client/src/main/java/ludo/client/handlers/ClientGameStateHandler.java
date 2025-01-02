package ludo.client.handlers;

import ludo.client.events.GameStateUpdateEvent;
import ludo.client.screens.GameScreen;

// Client-side game state handler
public class ClientGameStateHandler {
    private final GameScreen gameScreen;

    public ClientGameStateHandler(GameScreen gameScreen) {
        this.gameScreen = gameScreen;
    }

    public void handleStateUpdate(GameStateUpdateEvent event) {
        // Update UI based on new state
        event.getPawnPositions().forEach((color, positions) -> {
            gameScreen.updatePlayerPawns(color, positions);
        });

        gameScreen.setCurrentPlayer(event.getCurrentPlayer());
        gameScreen.updateGameState(event.getGameState());
    }

    public void handleTurnChange(String currentPlayer) {
        gameScreen.setCurrentPlayer(currentPlayer);
        if (currentPlayer.equals(gameScreen.getCurrentPlayer().getName())) {
            gameScreen.enableControls();
        } else {
            gameScreen.disableControls();
        }
    }
}
