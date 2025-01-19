package ludo.client.handlers;

import ludo.core.events.serverToClient.GameStateUpdateEvent;
import ludo.client.screens.GameScreen;

// Client-side game state handlers
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
        gameScreen.updateGameState(event.getGameState().toString()); //TODO check if correct
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
