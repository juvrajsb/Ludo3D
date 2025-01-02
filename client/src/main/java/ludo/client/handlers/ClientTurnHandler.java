package ludo.client.handlers;

import ludo.client.events.DiceRollResultEvent;
import ludo.client.screens.GameScreen;

// Client-side turn handler
public class ClientTurnHandler {
    private final GameScreen gameScreen;
    private String currentPlayerName;

    public ClientTurnHandler(GameScreen gameScreen) {
        this.gameScreen = gameScreen;
    }

    public void handleTurnStart(String playerName) {
        currentPlayerName = playerName;
        if (isLocalPlayerTurn()) {
            gameScreen.enableControls();
            gameScreen.showMessage("Your turn!");
        } else {
            gameScreen.disableControls();
            gameScreen.showMessage("Waiting for " + playerName);
        }
    }

    public boolean isLocalPlayerTurn() {
        return currentPlayerName != null &&
            currentPlayerName.equals(gameScreen.getCurrentPlayer().getName());
    }

    public void handleDiceRoll(DiceRollResultEvent event) {
        gameScreen.updateDiceDisplay(event.getValue());
        if (isLocalPlayerTurn()) {
            gameScreen.enablePawnSelection();
        }
    }
}

