package ludo.server.networking;

import ludo.core.events.Event;
import ludo.core.network.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ServerNetworkHandler {
    private static final Logger LOGGER = Logger.getLogger(ServerNetworkHandler.class.getName());
    private static final int MAX_CLIENTS = 4;
    private MessageListener messageListener;

    private final Map<String, Connection> clients;
//    private final int port;
    private ServerSocket serverSocket;
    private volatile boolean running;

    public ServerNetworkHandler(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        this.clients = new ConcurrentHashMap<>();
//        this.running = false;
    }

    public void start() {
        if (running) {
            return;
        }

        try {
//            serverSocket = new ServerSocket(port);
            running = true;
//            LOGGER.info("Server started on port " + port);
            LOGGER.info("Server started on port " + serverSocket.getLocalPort());
            acceptClients();
        } catch (Exception e) {
            LOGGER.severe("Failed to start server: " + e.getMessage());
            throw new RuntimeException("Server startup failed", e);
        }
    }

    public void stop() {
        running = false;
        try {
            // Close all client connections
            for (Connection client : clients.values()) {
                client.close();
            }
            clients.clear();

            if (!serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception e) {
            LOGGER.severe("Error stopping server: " + e.getMessage());
        }
    }

    private void acceptClients() {
        new Thread(() -> {
            while (running && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    if (clients.size() >= MAX_CLIENTS) {
                        clientSocket.close();
                        continue;
                    }
                    handleNewClient(clientSocket);
                } catch (Exception e) {
                    if (running) {
                        LOGGER.severe("Error accepting client: " + e.getMessage());
                    }
                }
            }
        }, "ClientAcceptor").start();
    }

    private void handleNewClient(Socket socket) {
        try {
            Connection client = Connection.createServerSide(socket);
            clients.put(client.getConnectionID(), client);

            startClientMessageHandling(client);

        } catch (Exception e) {
            LOGGER.severe("Error handling new client: " + e.getMessage());
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private void startClientMessageHandling(Connection client) {
        new Thread(() -> {
            while (running && !serverSocket.isClosed()) {
                try {
                    NetworkMessage message = client.receive();
                    handleClientMessage(client, message);
                } catch (Exception e) {
                    if (running) {
                        LOGGER.warning("Client error: " + e.getMessage());
                        handleClientError(client);
                        break;
                    }
                }
            }
        }, "Client-" + client.getConnectionID()).start();
    }

    private void handleClientMessage(Connection client, NetworkMessage message) {
        if (message instanceof Event) {
            ((Event) message).setConnection(client);
        }

        if (messageListener != null) {
            messageListener.onMessageReceived(message);
        }
    }

    private void handleClientError(Connection client) {
        if (messageListener != null) {
            messageListener.onConnectionError(
                new IOException("Client disconnected: " + client.getConnectionID())
            );
        }
        try {
            client.close();
            clients.remove(client.getConnectionID());
            LOGGER.info("Client disconnected: " + client.getConnectionID());
        } catch (IOException e) {
            LOGGER.severe("Error closing client connection: " + e.getMessage());
        }
    }

    public void broadcast(NetworkMessage message) {
        for (Connection client : clients.values()) {
            try {
                client.send(message);
            } catch (Exception e) {
                LOGGER.warning("Error broadcasting to client: " + e.getMessage());
                handleClientError(client);
            }
        }
    }

    public void sendToClient(String clientId, NetworkMessage message) {
        Connection client = clients.get(clientId);
        if (client != null) {
            try {
                client.send(message);
            } catch (Exception e) {
                LOGGER.warning("Error sending to client: " + e.getMessage());
                handleClientError(client);
            }
        }
    }

    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    public boolean hasClient(String clientId) {
        return clients.containsKey(clientId);
    }

    public int getClientCount() {
        return clients.size();
    }

//    public int getPort() {
//        return port;
//    }

    public void handlePong(String connectionId) {
        Connection connection = clients.get(connectionId);
        if (connection != null) {
            connection.resetPingFailure();
        }
    }

    public List<Connection> getActiveConnections() {
        return new ArrayList<>(clients.values());
    }
}
