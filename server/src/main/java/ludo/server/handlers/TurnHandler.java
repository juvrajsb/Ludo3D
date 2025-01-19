package ludo.server.handlers;

import ludo.core.game.GameManager;
import ludo.core.game.GameState;
import ludo.server.networking.EventTransmitter;
import ludo.server.Server;
import ludo.core.entities.Player;
import ludo.core.events.serverToClient.GameStateUpdateEvent;

import java.util.Timer;
import java.util.TimerTask;

public class TurnHandler {
    private static final int TURN_TIMEOUT_SECONDS = 30;
    private final GameManager gameManager;
    private final EventTransmitter eventTransmitter;
    private final GameStateHandler stateHandler;
    private Timer turnTimer;

    public TurnHandler() {
        this.gameManager = GameManager.getInstance();
        this.eventTransmitter = new EventTransmitter(Server.getInstance().getAllConnections());
        this.stateHandler = new GameStateHandler();
        this.turnTimer = new Timer();
    }

    public void startNewTurn() {
        Player currentPlayer = gameManager.getCurrentPlayer();
        stateHandler.updateGameState(GameState.IN_PROGRESS);

        // Cancel any existing timer
        if (turnTimer != null) {
            turnTimer.cancel();
        }
        turnTimer = new Timer();

        // Set timer for current turn
        turnTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                handleTurnTimeout();
            }
        }, TURN_TIMEOUT_SECONDS * 1000);

        // Update game state for all players
        broadcastTurnStart();
    }

    public void endTurn() {
        // Cancel turn timer
        if (turnTimer != null) {
            turnTimer.cancel();
        }

        // Move to next player
        gameManager.nextTurn();

        // Start new turn
        startNewTurn();
    }

    private void handleTurnTimeout() {
        Player currentPlayer = gameManager.getCurrentPlayer();
        Server.LOGGER.info("Turn timeout for player: " + currentPlayer.getName());

        // Auto-skip turn on timeout
        endTurn();
    }

    private void broadcastTurnStart() {
        Player currentPlayer = gameManager.getCurrentPlayer();
        GameStateUpdateEvent updateEvent = new GameStateUpdateEvent(
            gameManager.getCurrentPawnPositions(),
            currentPlayer.getColor(),
            GameState.IN_PROGRESS
        );

        try {
            eventTransmitter.broadcast(updateEvent);
        } catch (Exception e) {
            Server.LOGGER.severe("Failed to broadcast turn start: " + e.getMessage());
        }
    }

    public void pauseTurnTimer() {
        if (turnTimer != null) {
            turnTimer.cancel();
        }
    }

    public void resumeTurnTimer() {
        startNewTurn();
    }

    public boolean isPlayerTurn(String playerName) {
        Player currentPlayer = gameManager.getCurrentPlayer();
        return currentPlayer != null && currentPlayer.getName().equals(playerName);
    }
}
