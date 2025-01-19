package ludo.server.handlers;

import ludo.core.game.GameManager;
import ludo.core.game.GameState;
import ludo.server.networking.EventTransmitter;
import ludo.server.Server;
import ludo.core.entities.Player;
import ludo.core.events.serverToClient.GameStateUpdateEvent;
import ludo.core.events.serverToClient.WinnerProclamationEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GameStateHandler {
    private final GameManager gameManager;
    private final EventTransmitter eventTransmitter;
    private GameState currentState;

    public GameStateHandler() {
        this.gameManager = GameManager.getInstance();
        this.eventTransmitter = new EventTransmitter(Server.getInstance().getAllConnections());
        this.currentState = GameState.WAITING_FOR_PLAYERS;
    }

    public void updateGameState(GameState newState) {
        if (!isValidStateTransition(newState)) {
            Server.LOGGER.warning("Invalid state transition attempted: " +
                currentState + " -> " + newState);
            return;
        }

        currentState = newState;
        broadcastGameState();
    }

    private boolean isValidStateTransition(GameState newState) {
        switch (currentState) {
            case WAITING_FOR_PLAYERS:
                return newState == GameState.IN_PROGRESS;

            case IN_PROGRESS:
                return newState == GameState.DICE_ROLLED ||
                    newState == GameState.GAME_OVER;

            case DICE_ROLLED:
                return newState == GameState.WAITING_FOR_MOVE;

            case WAITING_FOR_MOVE:
                return newState == GameState.PLAYER_MOVED ||
                    newState == GameState.IN_PROGRESS;

            case PLAYER_MOVED:
                return newState == GameState.IN_PROGRESS ||
                    newState == GameState.GAME_OVER;

            case GAME_OVER:
                return false; // No transitions allowed from game over

            default:
                return false;
        }
    }

    public void broadcastGameState() {
        Map<String, List<Integer>> pawnPositions = new HashMap<>();

        // Collect current positions for all players
        for (Player player : gameManager.getPlayers()) {
            List<Integer> positions = player.getPawns().stream()
                .map(pawn -> pawn.getPosition())
                .collect(Collectors.toList());
            pawnPositions.put(player.getColor(), positions);
        }

        GameStateUpdateEvent stateEvent = new GameStateUpdateEvent(
            pawnPositions,
            gameManager.getCurrentPlayer().getColor(),
            currentState
        );

        try {
            eventTransmitter.broadcast(stateEvent);
        } catch (Exception e) {
            Server.LOGGER.severe("Failed to broadcast game state: " + e.getMessage());
        }
    }

    public void handleGameEnd(Player winner) {
        updateGameState(GameState.GAME_OVER);

        // Create winner proclamation event
        WinnerProclamationEvent winEvent = new WinnerProclamationEvent(
            Collections.singletonList(winner.getName())
        );

        try {
            eventTransmitter.broadcast(winEvent);
        } catch (Exception e) {
            Server.LOGGER.severe("Failed to broadcast winner: " + e.getMessage());
        }
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public boolean isGameInProgress() {
        return currentState != GameState.WAITING_FOR_PLAYERS &&
            currentState != GameState.GAME_OVER;
    }
}
