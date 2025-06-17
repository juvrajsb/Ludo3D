package ludo.server.networking;

import ludo.core.network.Connection;
import ludo.server.Server;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager{
    private static ConnectionManager instance;
    private final Map<String, Connection> connections;
    private final EventTransmitter eventTransmitter;

    private ConnectionManager(Server server) {
        this.connections = new ConcurrentHashMap<>();
        this.eventTransmitter = new EventTransmitter(new ArrayList<>(connections.values()));
    }

    public static synchronized ConnectionManager getInstance(Server server) {
        if (instance == null) {
            instance = new ConnectionManager(server);
        }
        return instance;
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

    public void handleDisconnection(String connectionId) {
        removeConnection(connectionId);
    }
}
