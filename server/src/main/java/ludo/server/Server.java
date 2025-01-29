package ludo.server;

import ludo.core.network.Connection;
import ludo.server.networking.*;
import ludo.server.state.ServerGameStateManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class Server {
    public static final Logger LOGGER = Logger.getLogger(Server.class.getName());
    private static final int DEFAULT_PORT = 12000;

    private static Server instance;
    private final ServerSocket welcomeSocket;
    private final ServerNetworkHandler networkHandler;
    private final ServerGameStateManager gameStateManager;
    private final EventReceiver eventReceiver;
    private final Map<Connection, Thread> connectedClients;
    private volatile boolean running;

    private Server(int port) {
        try {
            this.welcomeSocket = new ServerSocket(port);
            this.networkHandler = new ServerNetworkHandler(welcomeSocket);
            this.gameStateManager = new ServerGameStateManager(networkHandler);
            this.eventReceiver = new EventReceiver();
            this.connectedClients = new ConcurrentHashMap<>();
            this.running = false;
        } catch (IOException e) {
            LOGGER.severe("Could not create server socket on port " + port);
            throw new RuntimeException(e);
        }
    }

    public static synchronized Server getInstance(int port) {
        if (instance == null) {
            instance = new Server(port);
        }
        return instance;
    }

    public static Server getInstance() {
        return getInstance(DEFAULT_PORT);
    }

    public void start() {
        if (running) {
            LOGGER.warning("Server is already running");
            return;
        }

        try {
            networkHandler.start();
            running = true;
            // Start network listener and event receiver in separate threads
            new Thread(new NetworkListener(this), "NetworkListener").start();
            new Thread(eventReceiver, "EventReceiver").start();
            LOGGER.info("Server started on port " + welcomeSocket.getLocalPort());
        } catch (Exception e) {
            LOGGER.severe("Failed to start server: " + e.getMessage());
            throw new RuntimeException("Server startup failed", e);
        }
    }

    public void stop() {
        if (!running) {
            return;
        }

        try {
            running = false;
            // Close all client connections
            for (Map.Entry<Connection, Thread> entry : connectedClients.entrySet()) {
                try {
                    entry.getKey().close();
                    entry.getValue().interrupt();
                } catch (IOException e) {
                    LOGGER.warning("Error closing client connection: " + e.getMessage());
                }
            }
            connectedClients.clear();
            networkHandler.stop();
            welcomeSocket.close();
            LOGGER.info("Server stopped");
        } catch (Exception e) {
            LOGGER.severe("Error stopping server: " + e.getMessage());
        }
    }

    // Methods needed by NetworkListener
    public ServerSocket getWelcomeSocket() {
        return welcomeSocket;
    }

    public EventReceiver getEventReceiver() {
        return eventReceiver;
    }

    public synchronized void addClient(Connection connection, Thread clientThread) {
        connectedClients.put(connection, clientThread);
    }

    public synchronized void removeClient(Connection connection) {
        Thread clientThread = connectedClients.remove(connection);
        if (clientThread != null) {
            clientThread.interrupt();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public List<Connection> getAllConnections() {
        return connectedClients.keySet().stream().toList();
    }

    public ServerGameStateManager getGameStateManager() {
        return gameStateManager;
    }

    public ServerNetworkHandler getNetworkHandler() {
        return networkHandler;
    }
}
//package ludo.server;
//
//import ludo.core.events.Event;
//import ludo.core.network.Connection;
//import ludo.core.network.MessageListener;
//import ludo.core.network.NetworkMessage;
//import ludo.server.networking.*;
//import ludo.server.state.ServerGameStateManager;
//import ludo.core.events.clientToServer.*;
//
//import java.io.IOException;
//import java.net.ServerSocket;
//import java.util.Map;
//import java.util.List;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.logging.Logger;
//
//public class Server implements MessageListener {
//    public static final Logger LOGGER = Logger.getLogger(Server.class.getName());
//    private static final int DEFAULT_PORT = 12000;
//
//    private static Server instance;
//    private final ServerSocket welcomeSocket;
//    private final ServerNetworkHandler networkHandler;
//    private final ServerGameStateManager gameStateManager;
//    private final EventReceiver eventReceiver;
//    private final Map<Connection, Thread> connectedClients;
//    private volatile boolean running;
//
//    private Server(int port) {
//        try {
//            this.welcomeSocket = new ServerSocket(port);
//            this.networkHandler = new ServerNetworkHandler(port);
//            this.gameStateManager = new ServerGameStateManager(networkHandler);
//            this.eventReceiver = new EventReceiver();
//            this.connectedClients = new ConcurrentHashMap<>();
//            this.running = false;
//        } catch (IOException e) {
//            LOGGER.severe("Could not create server socket on port " + port);
//            throw new RuntimeException(e);
//        }
//    }
//
//    public static synchronized Server getInstance(int port) {
//        if (instance == null) {
//            instance = new Server(port);
//        }
//        return instance;
//    }
//
//    public static Server getInstance() {
//        return getInstance(DEFAULT_PORT);
//    }
//
//    public void start() {
//        if (running) {
//            LOGGER.warning("Server is already running");
//            return;
//        }
//
//        try {
//            networkHandler.setMessageListener(this);
//            networkHandler.start();
//            running = true;
//            // Start network listener and event receiver in separate threads
//            new Thread(new NetworkListener(this), "NetworkListener").start();
//            new Thread(eventReceiver, "EventReceiver").start();
//            LOGGER.info("Server started on port " + welcomeSocket.getLocalPort());
//        } catch (Exception e) {
//            LOGGER.severe("Failed to start server: " + e.getMessage());
//            throw new RuntimeException("Server startup failed", e);
//        }
//    }
//
//    public void stop() {
//        if (!running) {
//            return;
//        }
//
//        try {
//            running = false;
//            // Close all client connections
//            for (Map.Entry<Connection, Thread> entry : connectedClients.entrySet()) {
//                entry.getKey().close();
//                entry.getValue().interrupt();
//            }
//            connectedClients.clear();
//            networkHandler.stop();
//            welcomeSocket.close();
//            LOGGER.info("Server stopped");
//        } catch (Exception e) {
//            LOGGER.severe("Error stopping server: " + e.getMessage());
//        }
//    }
//
//    @Override
//    public void onMessageReceived(NetworkMessage message) {
//        try {
//            if (message instanceof Event event) {
//                handleMessage(event);  // Now we pass the Event which has connection info
//            } else {
//                LOGGER.warning("Received non-event message: " + message.getType());
//            }
//        } catch (Exception e) {
//            LOGGER.severe("Error handling message: " + e.getMessage());
//        }
//    }
//
//    @Override
//    public void onConnectionError(Exception e) {
//        LOGGER.severe("Connection error: " + e.getMessage());
//    }
//
//    // Methods needed by NetworkListener
//    public ServerSocket getWelcomeSocket() {
//        return welcomeSocket;
//    }
//
//    public EventReceiver getEventReceiver() {
//        return eventReceiver;
//    }
//
//    public void addClient(Connection connection, Thread clientThread) {
//        connectedClients.put(connection, clientThread);
//    }
//
//    public boolean isRunning() {
//        return running;
//    }
//
//    public List<Connection> getAllConnections() {
//        return connectedClients.keySet().stream().toList();
//    }
//
//    // Helper methods
//    private void handleMessage(Event event) {
//        Connection connection = event.getConnection();
//        if (connection == null) {
//            LOGGER.warning("Received message without connection");
//            return;
//        }
//
//        try {
//            switch (event.getType()) {
//                case "JOIN_GAME_REQUEST" -> handleJoinRequest((JoinGameRequestEvent) event, connection);
//                case "LEAVE_GAME_REQUEST" -> gameStateManager.handlePlayerLeave(connection.getConnectionID());
//                case "DICE_ROLL_REQUEST" -> gameStateManager.handleDiceRoll(connection.getConnectionID());
//                case "MOVE_REQUEST" -> {
//                    MoveRequestEvent moveEvent = (MoveRequestEvent) event;
//                    gameStateManager.handleMove(connection.getConnectionID(),
//                        moveEvent.getPawnIndex(),
//                        moveEvent.getSteps());
//                }
//                case "PONG" -> networkHandler.handlePong(connection.getConnectionID());
//                default -> LOGGER.warning("Unknown message type: " + event.getType());
//            }
//        } catch (Exception e) {
//            LOGGER.severe("Error processing message: " + e.getMessage());
//            handleClientError(connection, e);
//        }
//    }
//
//    private void handleJoinRequest(JoinGameRequestEvent event, Connection connection) {
//        boolean joined = gameStateManager.handlePlayerJoin(
//            connection.getConnectionID(),
//            event.getPlayerName(),
//            event.getDesiredColor()
//        );
//        if (!joined) {
//            LOGGER.info("Player failed to join: " + event.getPlayerName());
//        }
//    }
//
//    private void handleClientError(Connection connection, Exception error) {
//        LOGGER.warning("Client error for " + connection.getConnectionID() + ": " + error.getMessage());
//        if (error instanceof IOException) {
//            gameStateManager.handleDisconnection(connection.getConnectionID());
//        }
//    }
//}
