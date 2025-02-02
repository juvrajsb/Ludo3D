package ludo.client.networking;

import ludo.core.network.*;
import java.io.IOException;
// import java.util.Timer;
import java.util.Objects;
import java.util.logging.Logger;
import ludo.core.events.serverToClient.PingEvent;
import ludo.core.events.clientToServer.PongEvent;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class is responsible for handling the client network.
 */
public class ClientNetworkHandler implements NetworkHandler {
    private static final Logger LOGGER = Logger.getLogger(ClientNetworkHandler.class.getName());
    private static final int RECONNECT_ATTEMPTS = 3;
    private static final int RECONNECT_DELAY = 5000; // 5 seconds

    private Connection connection;
    private MessageListener listener;
    private final AtomicBoolean isConnected;
    private Thread listenerThread;
    private volatile boolean running;

    public ClientNetworkHandler() {
        this.isConnected = new AtomicBoolean(false);
    }

//    private void handleConnectionFailure(Exception error) {
//        if (!isConnected) {
//            return;
//        }
//
//        LOGGER.warning("Connection failure: " + error.getMessage());
//        error.printStackTrace();
//
//        if (listener != null) {
//            listener.onConnectionError(error);
//        }
//
//        // Try to recover connection
//        attemptReconnect();
//    }
//
//    private void attemptReconnect() {
//        for (int attempt = 0; attempt < RECONNECT_ATTEMPTS && isConnected; attempt++) {
//            try {
//                LOGGER.info("Attempting reconnect: " + (attempt + 1) + "/" + RECONNECT_ATTEMPTS);
//                Thread.sleep(RECONNECT_DELAY);
//
//                // Stop existing services but maintain isConnected flag
//                stopAllServices();
//
//                // Try to reconnect
//                connection = Connection.createClientSide(connection.getHost(), connection.getPort());
//                startPingMonitoring();
//                startListening();
//
//                LOGGER.info("Reconnected successfully!");
//                return;
//
//            } catch (Exception e) {
//                LOGGER.warning("Reconnect attempt failed: " + e.getMessage());
//            }
//        }
//
//        // If we get here, all reconnect attempts failed
//        LOGGER.severe("Failed to reconnect after " + RECONNECT_ATTEMPTS + " attempts");
//        disconnect();
//    }
//
//    private void stopAllServices() {
//        if (pingSender != null) {
//            pingSender.stop();
//            pingSender = null;
//        }
//
//        if (listenerThread != null) {
//            listenerThread.interrupt();
//            listenerThread = null;
//        }
//
//        if (connection != null) {
//            try {
//                connection.close();
//            } catch (Exception e) {
//                LOGGER.warning("Error closing connection: " + e.getMessage());
//            }
//            connection = null;
//        }
//    }
//
//    public String getPlayerId() {
//        return null;
//    }
//
//
//    public void stop() {
//        // Add necessary logic to stop the network handler
//        if (pingSender != null) {
//            pingSender.stop();
//            pingSender = null;
//        }
//
//        if (connection != null) {
//            try {
//                connection.close();
//            } catch (IOException e) {
//                LOGGER.severe("Error closing connection: " + e.getMessage());
//            }
//        }
//        if (listenerThread != null) {
//            listenerThread.interrupt();
//        }
//        isConnected = false;
//    }
//
//    public void start(String connectionID, int port) {
//        // Add necessary logic to start the network handler
//        try {
//            connection = Connection.createClientSide(connectionID, port);
//            startPingMonitoring();
//            startListening();
//            isConnected = true;
//            LOGGER.info("Started network handler with connection ID: " + connectionID);
//        } catch (Exception e) {
//            LOGGER.severe("Failed to start network handler: " + e.getMessage());
//            handleConnectionFailure(e);
//        }
//    }

    @Override
    public synchronized void connect(String host, int port) {
        if (isConnected.get()) {
            LOGGER.warning("Already connected");
            return;
        }

        try {
            connection = Connection.createClientSide(host, port);
            startListening();
            isConnected.set(true);
            running = true;
            LOGGER.info("Connected to server at " + host + ":" + port);
        } catch (Exception e) {
            LOGGER.severe("Failed to connect: " + e.getMessage());
            disconnect();
            throw new RuntimeException("Connection failed", e);
        }
    }

    @Override
    public synchronized void disconnect() {
        running = false;
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException e) {
                LOGGER.warning("Error closing connection: " + e.getMessage());
            }
            connection = null;
        }
        if (listenerThread != null) {
            listenerThread.interrupt();
            listenerThread = null;
        }
        isConnected.set(false);
        LOGGER.info("Disconnected from server");
    }

    private void startListening() {
        listenerThread = new Thread(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    NetworkMessage message = connection.receive();
                    handleMessage(message);
                } catch (IOException e) {
                    if (running) {
                        LOGGER.severe("Connection lost: " + e.getMessage());
                        handleConnectionError(e);
                        break;
                    }
                } catch (Exception e) {
                    LOGGER.severe("Error processing message: " + e.getMessage());
                }
            }
        }, "NetworkListener");
        listenerThread.start();
    }

    private void handleMessage(NetworkMessage message) {
        if(!Objects.equals(message.getType(), "PING")){LOGGER.info("Received message: " + message.getType());}

        if (message instanceof PingEvent) {
            handlePing();
            return;
        }

        if (listener != null) {
            try {
                listener.onMessageReceived(message);
            } catch (Exception e) {
                LOGGER.warning("Error in message listener: " + e.getMessage());
            }
        }
    }

    private void handlePing() {
        try {
            sendMessage(new PongEvent());
        } catch (Exception e) {
            LOGGER.warning("Failed to send pong: " + e.getMessage());
        }
    }

    private void handleConnectionError(Exception error) {
        isConnected.set(false);
        if (listener != null) {
            listener.onConnectionError(error);
        }
        disconnect();
    }

    @Override
    public void sendMessage(NetworkMessage message) {
        if (!isConnected.get() || connection == null) {
            throw new IllegalStateException("Not connected to server");
        }

        try {
            connection.send(message);
        } catch (Exception e) {
            LOGGER.severe("Failed to send message: " + e.getMessage());
            handleConnectionError(e);
            throw new RuntimeException("Send failed", e);
        }
    }

    @Override
    public void setMessageListener(MessageListener listener) {
        this.listener = listener;
    }

    public boolean isConnected() {
        return isConnected.get() && connection != null && !connection.isFailed();
    }

}
