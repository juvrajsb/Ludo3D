package ludo.client.handlers;

import ludo.client.GameStateManager;
import ludo.client.screens.GameScreen;
import ludo.core.entities.Player;
import ludo.core.events.Event;
import ludo.core.events.serverToClient.GameStartedEvent;

public class GameStartedHandler implements IClientEventHandler {
    @Override
    public void handle(GameStateManager gsm, Event event) {
        GameStartedEvent gameStartedEvent = (GameStartedEvent) event;

        gsm.setGameStarted(true);
        gsm.getCurrentPlayers().clear();
        gsm.getCurrentPlayers().addAll(gameStartedEvent.getPlayers());

        GameScreen newGameScreen = new GameScreen(gsm.getGame());
        gsm.setGameScreen(newGameScreen);
        gsm.getGame().setScreen(newGameScreen);

        newGameScreen.addPlayers(gsm.getCurrentPlayers());

        String startingPlayerName = gameStartedEvent.getStartingPlayer();
        gsm.setMyTurn(gsm.getCurrentUsername().equals(startingPlayerName));

        String startingPlayerColor = gsm.getCurrentPlayers().stream()
            .filter(p -> p.getName().equals(startingPlayerName))
            .findFirst()
            .map(Player::getColor)
            .orElse("");

        newGameScreen.setCurrentPlayer(startingPlayerColor);

        if (gsm.isMyTurn()) {
            newGameScreen.enableControls();
            newGameScreen.showMessage("Your turn!");
        } else {
            newGameScreen.disableControls();
            newGameScreen.showMessage("Waiting for " + startingPlayerColor);
        }
    }
}
