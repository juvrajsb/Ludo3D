package ludo.server.networking;

import ludo.core.events.Event;
import ludo.core.network.*;
import ludo.server.Server;

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

    private final Map<String, Connection> clients = new ConcurrentHashMap<>();
    private final Object clientsLock = new Object(); // For thread-safe client operations
    //    private final int port;
    private ServerSocket serverSocket;
    private volatile boolean running;
    private final Server server;

    public ServerNetworkHandler(Server server, ServerSocket serverSocket) {
        this.server = server;
        this.serverSocket = serverSocket;
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
//            acceptClients();
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

//    private void acceptClients() {
//        new Thread(() -> {
//            while (running && !serverSocket.isClosed()) {
//                try {
//                    Socket clientSocket = serverSocket.accept();
//                    if (clients.size() >= MAX_CLIENTS) {
//                        clientSocket.close();
//                        continue;
//                    }
//                    handleNewClient(clientSocket);
//                } catch (Exception e) {
//                    if (running) {
//                        LOGGER.severe("Error accepting client: " + e.getMessage());
//                    }
//                }
//            }
//        }, "ClientAcceptor").start();
//    }

//    public void handleNewClient(Socket clientSocket) {
//        try {
//            Connection client = Connection.createServerSide(clientSocket);
//            String clientId = client.getConnectionID();
//
//            synchronized(clientsLock) {
//                if (clients.size() >= MAX_CLIENTS) {
//                    client.close();
//                    return;
//                }
//                clients.put(clientId, client);
//                LOGGER.info("New client registered: " + clientId);
//            }
//
//            // Start client message handling in new thread
//            startClientMessageHandling(client);
//
//        } catch (Exception e) {
//            LOGGER.severe("Error handling new client: " + e.getMessage());
//            try {
//                clientSocket.close();
//            } catch (IOException ignored) {}
//        }
//    }


    private void handleClientMessage(Connection client, NetworkMessage message) {
        if (message instanceof Event) {
            ((Event) message).setConnection(client);
        }

        if (messageListener != null) {
            messageListener.onMessageReceived(message);
        }
    }

//    private void handleClientError(Connection client) {
//        String clientId = client.getConnectionID();
//
//        synchronized(clientsLock) {
//            if (clients.remove(clientId) != null) {
//                LOGGER.info("Client removed: " + clientId);
//
//                // Stop ping sender first
//                PingSender pingSender = client.getPingSender();
//                if (pingSender != null) {
//                    pingSender.stop();
//                }
//
//                // Close connection
//                try {
//                    client.close();
//                } catch (IOException e) {
//                    LOGGER.warning("Error closing client connection: " + e.getMessage());
//                }
//
//                // Notify message listener
//                if (messageListener != null) {
//                    messageListener.onConnectionError(
//                        new IOException("Client disconnected: " + clientId)
//                    );
//                }
//            }
//        }
//    }

    public void addClient(Connection connection) throws IOException {
        String clientId = connection.getConnectionID();

        if (clients.size() >= MAX_CLIENTS) {
            throw new IOException("Maximum clients reached");
        }

        // Register client
        clients.put(clientId, connection);
        LOGGER.info("New client registered: " + clientId);

        // Start message handling thread
        Thread clientThread = startClientMessageHandling(connection);

        // Initialize ping monitoring
        startPingMonitoring(connection);

        // Register with server
        server.addClient(connection, clientThread);
    }

    private Thread startClientMessageHandling(Connection connection) {
        Thread thread = new Thread(() -> {
            while (!Thread.interrupted() && !connection.isClosed()) {
                try {
                    NetworkMessage message = connection.receive();
                    if (message instanceof Event) {
                        ((Event) message).setConnection(connection);
                        if (messageListener != null) {
                            messageListener.onMessageReceived(message);
                        }
                    }
                } catch (Exception e) {
                    if (!connection.isClosed()) {
                        handleClientError(connection);
                    }
                    break;
                }
            }
        }, "MessageHandler-" + connection.getConnectionID());

        thread.start();
        return thread;
    }

    private void startPingMonitoring(Connection connection) {
        ServerPingSender pingSender = new ServerPingSender(connection);
        connection.setPingSender(pingSender);
        pingSender.start();
    }

    private void handleClientError(Connection connection) {
        String clientId = connection.getConnectionID();
        if (clients.remove(clientId) != null) {
            LOGGER.info("Client disconnected: " + clientId);

            try {
                connection.close();
            } catch (IOException e) {
                LOGGER.warning("Error closing client connection: " + e.getMessage());
            }

            server.removeClient(connection);

            if (messageListener != null) {
                messageListener.onConnectionError(
                    new IOException("Client disconnected: " + clientId)
                );
            }
        }
    }

    public void removeClient(String clientId) {
        synchronized(clientsLock) {
            Connection connection = clients.remove(clientId);
            if (connection != null) {
                LOGGER.info("Client unregistered: " + clientId);
                try {
                    if (connection.getPingSender() != null) {
                        connection.getPingSender().stop();
                    }
                    connection.close();
                } catch (IOException e) {
                    LOGGER.warning("Error closing connection for " + clientId + ": " + e.getMessage());
                }
            }
        }
    }

//    private void startClientMessageHandling(Connection client) {
//        new Thread(() -> {
//            while (running && !client.isClosed()) {
//                try {
//                    NetworkMessage message = client.receive();
//                    if (message instanceof Event) {
//                        ((Event) message).setConnection(client);
//                    }
//                    if (messageListener != null) {
//                        messageListener.onMessageReceived(message);
//                    }
//                } catch (Exception e) {
//                    if (running) {
//                        LOGGER.warning("Client error: " + e.getMessage());
//                        handleClientError(client);
//                        break;
//                    }
//                }
//            }
//        }, "ClientHandler-" + client.getConnectionID()).start();
//    }

    public int getClientCount() {
        synchronized(clientsLock) {
            return clients.size();
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
