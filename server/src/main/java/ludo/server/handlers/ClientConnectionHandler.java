package ludo.server.handlers;

import ludo.core.network.Connection;
import ludo.server.networking.EventTransmitter;
import ludo.server.Server;
import ludo.core.events.clientToServer.ClientDisconnectedEvent;
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
//
//    public void handleNewConnection(Connection connection) {
//        // Add to active connections
//        activeConnections.put(connection.getConnectionID(), connection); //TODO check, no getConnectionID method in Connection
//        Server.LOGGER.info("New client connected: " + connection.getConnectionID());
//    }
//
//    public void handleDisconnection(ClientDisconnectedEvent event) throws IOException {
//        Connection connection = event.getConnection(); //TODO check, no getConnection method in ClientDisconnectedEvent
//        String clientId = connection.getConnectionID();
//
//        // Remove from active connections
//        activeConnections.remove(clientId);
//
//        // Close connection
//        connection.close();
//
//        // Handle player removal if game hasn't started
//        playerJoinHandler.handlePlayerDisconnect(clientId);
//
//        // Notify other players
//        notifyOtherPlayersOfDisconnection(clientId);
//
//        Server.LOGGER.info("Client disconnected: " + clientId);
//    }

    public void handleUnexpectedDisconnection(String clientId) {
        Connection connection = activeConnections.get(clientId);
        if (connection != null) {
            try {
                // Notify other players
                UnexceptedDisconnetionEvent disconnectionEvent = new UnexceptedDisconnetionEvent();
                eventTransmitter.broadcast(disconnectionEvent); //TODO check, provided UnexceptedDisconnetionEvent; required Event

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

//    private void notifyOtherPlayersOfDisconnection(String disconnectedClientId) {//TODO check usage not used currently
//        for (Connection connection : activeConnections.values()) {
//            if (!connection.getConnectionID().equals(disconnectedClientId)) {
//                try {
//                    UnexceptedDisconnetionEvent event = new UnexceptedDisconnetionEvent();
//                    connection.send(event);
//                } catch (IOException e) {
//                    Server.LOGGER.severe("Error notifying client of disconnection: " + e.getMessage());
//                }
//            }
//        }
//    }

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

//    public void closeAllConnections() {//TODO check usage not used currently
//        for (Connection connection : activeConnections.values()) {
//            try {
//                connection.close();
//            } catch (IOException e) {
//                Server.LOGGER.severe("Error closing connection: " + e.getMessage());
//            }
//        }
//        activeConnections.clear();
//    }
}
