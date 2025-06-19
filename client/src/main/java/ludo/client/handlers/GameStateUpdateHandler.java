package ludo.client.handlers;

import ludo.client.GameStateManager;
import ludo.client.screens.GameScreen;
import ludo.core.entities.Player;
import ludo.core.events.Event;
import ludo.core.events.serverToClient.GameStateUpdateEvent;
import ludo.core.game.GameState;

public class GameStateUpdateHandler implements IClientEventHandler {
    @Override
    public void handle(GameStateManager gsm, Event event) {
        GameStateUpdateEvent stateEvent = (GameStateUpdateEvent) event;
        GameScreen gameScreen = gsm.getGameScreen();
        if (gameScreen == null) return;

        stateEvent.getPawnPositions().forEach(gameScreen::updatePlayerPawns);
        gsm.setMyTurn(stateEvent.getCurrentPlayer().equals(gsm.getCurrentUsername()));

        String currentPlayerColor = gsm.getCurrentPlayers().stream()
            .filter(p -> p.getName().equals(stateEvent.getCurrentPlayer()))
            .findFirst()
            .map(Player::getColor)
            .orElse("Unknown");
        gameScreen.setCurrentPlayer(currentPlayerColor);

        if (gsm.isMyTurn()) {
            if (stateEvent.getGameState() == GameState.WAITING_FOR_MOVE) {
                gameScreen.enablePawnSelection();
                gameScreen.showMessage("Select a pawn to move.");
            } else if (stateEvent.getGameState() == GameState.IN_PROGRESS) {
                gameScreen.enableRollButton();
                gameScreen.showMessage("Your turn! Roll the dice.");
            } else {
                gameScreen.disableControls();
            }
        } else {
            gameScreen.disableControls();
            gameScreen.showMessage("Waiting for " + currentPlayerColor);
        }
    }
}
