package ludo.server.handlers;

import ludo.core.events.serverToClient.PlayerLeftEvent;
import ludo.server.Server;
import ludo.server.networking.EventTransmitter;
import ludo.core.network.Connection;
import ludo.core.events.serverToClient.DisconnectionAcknowledgedEvent;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientConnectionHandler {
    private final Map<String, Connection> activeConnections;
    private final PlayerJoinHandler playerJoinHandler;
    private final EventTransmitter eventTransmitter;

    public ClientConnectionHandler(Server server) {
        this.activeConnections = new ConcurrentHashMap<>();
        this.playerJoinHandler = new PlayerJoinHandler(server);
        this.eventTransmitter = new EventTransmitter(server.getAllConnections());
    }

    public void handleClientDisconnection(Connection connection) {
        if (connection == null) {
            Server.LOGGER.warning("Attempted to handle disconnection for null connection");
            return;
        }

        String clientId = connection.getConnectionID();
        Server.LOGGER.info("Handling client disconnection: " + clientId);

        try {
            // Stop ping sender first
            if (connection.getPingSender() != null) {
                connection.getPingSender().stop();
            }

            // Send acknowledgment
            try {
                DisconnectionAcknowledgedEvent ackEvent = new DisconnectionAcknowledgedEvent();
                connection.send(ackEvent);
            } catch (Exception e) {
                Server.LOGGER.severe("Failed to send disconnection acknowledgment: " + e.getMessage());
                return;
            }

            // Notify other players
            try {
                eventTransmitter.broadcast(new ludo.core.events.serverToClient.UnexceptedDisconnetionEvent());
            } catch (Exception e) {
                Server.LOGGER.warning("Failed to notify other players: " + e.getMessage());
            }

            // Close connection
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (IOException e) {
                Server.LOGGER.severe("Error closing connection: " + e.getMessage());
            }
        } catch (Exception e) {
            Server.LOGGER.severe("Error during disconnection: " + e.getMessage());
        } finally {
            // Cleanup
            activeConnections.remove(clientId);
            playerJoinHandler.handlePlayerDisconnect(clientId);
        }
    }
    public void handleUnexpectedDisconnection(Connection connection) {
        if (connection == null) return;

        String clientId = connection.getConnectionID();
        Server.LOGGER.info("Handling unexpected disconnection for: " + clientId);

        try {
            // Stop ping sender first
            if (connection.getPingSender() != null) {
                connection.getPingSender().stop();
            }

            // Close the connection
            if (!connection.isClosed()) {
                connection.close();
                Server.LOGGER.info("Closed connection for client: " + clientId);
            }
        } catch (Exception e) {
            Server.LOGGER.severe("Error during unexpected disconnection: " + e.getMessage());
        } finally {
            // This part is crucial and will now always execute
            activeConnections.remove(clientId);
            playerJoinHandler.handlePlayerDisconnect(clientId);

            // Notify other players about the disconnection
            eventTransmitter.broadcast(new PlayerLeftEvent(clientId));
        }
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
