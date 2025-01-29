package ludo.client.handlers;

import ludo.core.entities.Player;
import ludo.client.screens.GameScreen;
import ludo.core.events.serverToClient.DiceRollResultEvent;
import ludo.core.events.serverToClient.GameStartEvent;
import ludo.core.events.serverToClient.GameStateUpdateEvent;
import ludo.core.events.serverToClient.MoveResultEvent;
/**
 * This class is responsible for handling the game events.
 */
public class GameEventHandler {
    private final GameScreen gameScreen;

    public GameEventHandler(GameScreen gameScreen) {
        this.gameScreen = gameScreen;
    }

    public void handleDiceRollResult(DiceRollResultEvent event) {
        // Update dice display
        gameScreen.updateDiceDisplay(event.getValue());

        // If it's player's turn, enable pawn selection
        if (event.getPlayerColor().equals(gameScreen.getCurrentPlayer().getColor())) {
            gameScreen.enablePawnSelection();
        }
    }

    public void handleMoveResult(MoveResultEvent event) {
        if (event.isSuccess()) {
            // Update pawn position on screen
            gameScreen.updatePawnPosition(
                event.getPawnIndex(),
                event.getNewPosition()
            );

            // Play move animation
            gameScreen.playMoveAnimation(
                event.getPawnIndex(),
                event.getNewPosition()
            );
        } else {
            // Show error message
            gameScreen.showMessage(event.getMessage());
        }
    }

    public void handleGameStateUpdate(GameStateUpdateEvent event) {
        // Update all pawn positions
        event.getPawnPositions().forEach((color, positions) -> {
            gameScreen.updatePlayerPawns(color, positions);
        });

        // Update current player indicator
        gameScreen.setCurrentPlayer(event.getCurrentPlayer());

        // Update game state
        gameScreen.updateGameState(event.getGameState().toString()); //TODO need to check
    }

    public void handleGameStart(GameStartEvent event) {
        // Initialize game with players
        for (Player player : event.getPlayers()) {
            gameScreen.addPlayer(player);
        }

        // Set starting player
        gameScreen.setCurrentPlayer(event.getStartingPlayer());

        // Enable game controls if it's player's turn
        if (event.getStartingPlayer().equals(gameScreen.getCurrentPlayer().getName())) {
            gameScreen.enableControls();
        }
    }
}
