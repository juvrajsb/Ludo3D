package ludo.server.handlers;

import ludo.core.network.Connection;
import ludo.server.networking.EventTransmitter;
import ludo.server.Server;
import ludo.core.events.serverToClient.UnexceptedDisconnetionEvent;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientConnectionHandler {
    private final Map<String, Connection> activeConnections;
    private final PlayerJoinHandler playerJoinHandler;
    private final EventTransmitter eventTransmitter;

    public ClientConnectionHandler() {
        this.activeConnections = new ConcurrentHashMap<>();
        this.playerJoinHandler = new PlayerJoinHandler();
        this.eventTransmitter = new EventTransmitter(Server.getInstance().getAllConnections());
    }

    public void handleUnexpectedDisconnection(String clientId) {
        Connection connection = activeConnections.get(clientId);
        if (connection != null) {
            try {
                // Notify other players
                UnexceptedDisconnetionEvent disconnectionEvent = new UnexceptedDisconnetionEvent();
                eventTransmitter.broadcast(disconnectionEvent);

                // Close connection
                connection.close();
            } catch (IOException e) {
                Server.LOGGER.severe("Error handling unexpected disconnection: " + e.getMessage());
            }

            // Remove from active connections
            activeConnections.remove(clientId);

            // Handle player removal
            playerJoinHandler.handlePlayerDisconnect(clientId);
        }
    }

    public boolean isClientConnected(String clientId) {
        return activeConnections.containsKey(clientId);
    }

    public void handleReconnection(String clientId, Connection newConnection) {
        if (activeConnections.containsKey(clientId)) {
            // Close old connection if it exists
            try {
                Connection oldConnection = activeConnections.get(clientId);
                oldConnection.close();
            } catch (IOException e) {
                Server.LOGGER.warning("Error closing old connection: " + e.getMessage());
            }
        }

        // Add new connection
        activeConnections.put(clientId, newConnection);
        Server.LOGGER.info("Client reconnected: " + clientId);
    }
}
