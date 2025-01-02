package ludo.server.handler;

import ludo.server.networking.Connection;
import ludo.server.networking.EventTransmitter;
import ludo.server.Server;
import ludo.server.game.GameManager;
import ludo.server.game.GameState;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NetworkErrorHandler {
    private static final int RECONNECT_TIMEOUT_SECONDS = 30;
    private static final int MAX_RECONNECT_ATTEMPTS = 3;

    private final Map<String, Integer> reconnectAttempts;
    private final Map<String, GameState> playerStates;
    private final ScheduledExecutorService scheduler;
    private final GameManager gameManager;
    private final ClientConnectionHandler connectionHandler;
    private final EventTransmitter eventTransmitter;

    public NetworkErrorHandler() {
        this.reconnectAttempts = new ConcurrentHashMap<>();
        this.playerStates = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.gameManager = GameManager.getInstance();
        this.connectionHandler = new ClientConnectionHandler();
        this.eventTransmitter = new EventTransmitter(Server.getInstance().getAllConnections());
    }

    public void handleConnectionError(Connection connection, Exception error) {
        String clientId = connection.getConnectionID();
        Server.LOGGER.warning("Connection error for client " + clientId + ": " + error.getMessage());

        // Store current game state for the player if in game
        if (gameManager.isGameStarted()) {
            playerStates.put(clientId, gameManager.getGameState());
        }

        // Initialize reconnection attempts if needed
        reconnectAttempts.putIfAbsent(clientId, 0);

        if (reconnectAttempts.get(clientId) < MAX_RECONNECT_ATTEMPTS) {
            scheduleReconnectionAttempt(clientId);
        } else {
            handlePermanentDisconnection(clientId);
        }
    }

    private void scheduleReconnectionAttempt(String clientId) {
        int currentAttempts = reconnectAttempts.get(clientId);
        reconnectAttempts.put(clientId, currentAttempts + 1);

        scheduler.schedule(() -> {
            if (!connectionHandler.isClientConnected(clientId)) {
                // If still disconnected after timeout, handle as permanent
                handlePermanentDisconnection(clientId);
            }
        }, RECONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private void handlePermanentDisconnection(String clientId) {
        Server.LOGGER.warning("Client " + clientId + " permanently disconnected after " +
            MAX_RECONNECT_ATTEMPTS + " reconnection attempts");

        // Clean up stored state
        reconnectAttempts.remove(clientId);
        playerStates.remove(clientId);

        // Handle player removal through connection handler
        connectionHandler.handleUnexpectedDisconnection(clientId);
    }

    public void handleReconnection(String clientId, Connection newConnection) {
        // Reset reconnection attempts
        reconnectAttempts.remove(clientId);

        // Restore player state if available
        GameState savedState = playerStates.remove(clientId);
        if (savedState != null && gameManager.isGameStarted()) {
            syncGameState(newConnection);
        }

        // Update connection in connection handler
        connectionHandler.handleReconnection(clientId, newConnection);

        Server.LOGGER.info("Client " + clientId + " successfully reconnected");
    }

    private void syncGameState(Connection connection) {
        try {
            // Send current game state to reconnected player
            gameManager.sendGameState(connection);
        } catch (IOException e) {
            Server.LOGGER.severe("Failed to sync game state for reconnected player: " +
                e.getMessage());
        }
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
