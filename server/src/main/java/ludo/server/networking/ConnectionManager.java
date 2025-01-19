package ludo.server.networking;

import ludo.core.events.Event;
import ludo.core.network.Connection;
import ludo.server.Server;
import ludo.server.handlers.NetworkErrorHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager{
    private static ConnectionManager instance;
    private final Map<String, Connection> connections;
    private final NetworkErrorHandler errorHandler;
    private final EventTransmitter eventTransmitter;

    private ConnectionManager() {
        this.connections = new ConcurrentHashMap<>();
        this.errorHandler = new NetworkErrorHandler();
        this.eventTransmitter = new EventTransmitter(new ArrayList<>(connections.values()));
    }

    public static synchronized ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }

    public void addConnection(Connection connection) {
        connections.put(connection.getConnectionID(), connection);
        initializeConnection(connection);
    }

    private void initializeConnection(Connection connection) {
        // Start ping monitoring
        ServerPingSender pingSender = new ServerPingSender(connection);
        connection.setPingSender(pingSender);
        pingSender.start();
    }

    public void removeConnection(String connectionId) {
        Connection connection = connections.remove(connectionId);
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException e) {
                Server.LOGGER.warning("Error closing connection: " + e.getMessage());
            }
        }
    }

    public void broadcastEvent(Event event) {
        List<String> failedConnections = new ArrayList<>();

        for (Map.Entry<String, Connection> entry : connections.entrySet()) {
            try {
                entry.getValue().send(event);
            } catch (IOException e) {
                failedConnections.add(entry.getKey());
                errorHandler.handleConnectionError(entry.getValue(), e);
            }
        }

        // Clean up failed connections
        failedConnections.forEach(this::removeConnection);
    }

    public void sendToPlayer(String playerId, Event event) {
        Connection connection = connections.get(playerId);
        if (connection != null) {
            try {
                connection.send(event);
            } catch (IOException e) {
                errorHandler.handleConnectionError(connection, e);
                removeConnection(playerId);
            }
        }
    }

    public boolean isConnected(String playerId) {
        return connections.containsKey(playerId);
    }

    public List<Connection> getAllConnections() {
        return new ArrayList<>(connections.values());
    }

    public void handleReconnection(String playerId, Connection newConnection) {
        removeConnection(playerId);
        addConnection(newConnection);
        errorHandler.handleReconnection(playerId, newConnection);
    }

    public void shutdown() {
        for (Connection connection : connections.values()) {
            try {
                connection.close();
            } catch (IOException e) {
                Server.LOGGER.warning("Error during connection shutdown: " + e.getMessage());
            }
        }
        connections.clear();
        errorHandler.shutdown();
    }

    public EventTransmitter getEventTransmitter() {
        return eventTransmitter;
    }

    public void handleDisconnection(String connectionId) {
        removeConnection(connectionId);
    }
}
