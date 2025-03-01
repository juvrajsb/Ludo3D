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
    private final NetworkListener networkListener;
    private final ServerGameStateManager gameStateManager;
    private final EventReceiver eventReceiver;
    private final Map<Connection, Thread> connectedClients;
    private volatile boolean running;

    private Server(int port) {
        try {
            this.welcomeSocket = new ServerSocket(port);
            this.networkHandler = new ServerNetworkHandler(this, welcomeSocket);
            this.gameStateManager = new ServerGameStateManager(networkHandler);
            this.networkListener = new NetworkListener(this);
            LOGGER.info("Server started on port " + welcomeSocket.getLocalPort());
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
            networkHandler.setMessageListener(gameStateManager);
            networkHandler.start();
            running = true;

            // Start network listener in separate thread
            Thread listenerThread = new Thread(networkListener, "NetworkListener");
            listenerThread.start();

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

        try {
            welcomeSocket.close();
        } catch (IOException e) {
            LOGGER.severe("Error stopping server: " + e.getMessage());
        }

        LOGGER.info("Server stopped");
    }

    // Methods needed by NetworkListener
    public ServerSocket getWelcomeSocket() {
        return welcomeSocket;
    }

//    public EventReceiver getEventReceiver() {
//        return eventReceiver;
//    }

    public synchronized void addClient(Connection connection, Thread clientThread) {
        connectedClients.put(connection, clientThread);
        LOGGER.info("Client fully initialized: " + connection.getConnectionID());
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

//    public ServerGameStateManager getGameStateManager() { //TODO check usage not used currently
//        return gameStateManager;
//    }

    public ServerNetworkHandler getNetworkHandler() {
        return networkHandler;
    }
}
