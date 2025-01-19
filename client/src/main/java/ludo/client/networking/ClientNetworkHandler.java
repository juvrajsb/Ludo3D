package ludo.client.networking;

import ludo.core.network.*;
import java.io.IOException;
import java.util.Timer;
import java.util.logging.Logger;

public class ClientNetworkHandler implements NetworkHandler {
    private static final Logger LOGGER = Logger.getLogger(ClientNetworkHandler.class.getName());
    private static final int RECONNECT_ATTEMPTS = 3;
    private static final int RECONNECT_DELAY = 5000; // 5 seconds

    private Connection connection;
    private boolean isConnected;
    private MessageListener listener;
//    private final Timer connectionEnsurer;
    private Thread listenerThread;
    private PingSender pingSender;


    public ClientNetworkHandler() {
//        this.connectionEnsurer = new Timer();
        this.isConnected = false;
    }

    @Override
    public void connect(String host, int port) {
        if (isConnected) {
            LOGGER.warning("Already connected, disconnect first");
            return;
        }

        try {
            connection = Connection.createClientSide(host, port);
            startPingMonitoring();
            startListening();
            isConnected = true;
            LOGGER.info("Connected to server!");
        } catch (Exception e) {
            LOGGER.severe("Failed to connect: " + e.getMessage());
            handleConnectionFailure(e);
        }
    }

    @Override
    public void disconnect() {
        if (!isConnected) {
            return;
        }

        try {
            stopAllServices();
            LOGGER.info("Disconnected from server");
        } finally {
            isConnected = false;
        }
    }

    @Override
    public void sendMessage(NetworkMessage message) {
        if (!isConnected || connection == null) {
            LOGGER.warning("Can't send message - not connected");
            return;
        }

        try {
            connection.send(message);
        } catch (Exception e) {
            LOGGER.severe("Failed to send message: " + e.getMessage());
            handleConnectionFailure(e);
        }
    }

    @Override
    public void setMessageListener(MessageListener listener) {
        this.listener = listener;
    }

    private void startPingMonitoring() {
        if (pingSender != null) {
            pingSender.stop();
        }
        pingSender = new ClientPingSender(connection);
        connection.setPingSender(pingSender);
        pingSender.start();
    }

    private void startListening() {
        listenerThread = new Thread(() -> {
            while (!Thread.interrupted() && isConnected) {
                try {
                    NetworkMessage message = connection.receive();
                    handleMessage(message);
                } catch (Exception e) {
                    if (isConnected) { // Only handle error if we haven't deliberately disconnected
                        handleConnectionFailure(e);
                        break;
                    }
                }
            }
        }, "NetworkListener");
        listenerThread.start();
    }

    private void handleMessage(NetworkMessage message) {
        if (listener != null) {
            try {
                listener.onMessageReceived(message);
            } catch (Exception e) {
                LOGGER.warning("Error in message listener: " + e.getMessage());
            }
        }
    }

    private void handleConnectionFailure(Exception error) {
        if (!isConnected) {
            return; // Already handling disconnect
        }

        LOGGER.warning("Connection failure: " + error.getMessage());

        if (listener != null) {
            listener.onConnectionError(error);
        }

        // Try to recover connection
        attemptReconnect();
    }

    private void attemptReconnect() {
        for (int attempt = 0; attempt < RECONNECT_ATTEMPTS && isConnected; attempt++) {
            try {
                LOGGER.info("Attempting reconnect: " + (attempt + 1) + "/" + RECONNECT_ATTEMPTS);
                Thread.sleep(RECONNECT_DELAY);

                // Stop existing services but maintain isConnected flag
                stopAllServices();

                // Try to reconnect
                connection = Connection.createClientSide(connection.getHost(), connection.getPort());
                startPingMonitoring();
                startListening();

                LOGGER.info("Reconnected successfully!");
                return;

            } catch (Exception e) {
                LOGGER.warning("Reconnect attempt failed: " + e.getMessage());
            }
        }

        // If we get here, all reconnect attempts failed
        LOGGER.severe("Failed to reconnect after " + RECONNECT_ATTEMPTS + " attempts");
        disconnect();
    }

    private void stopAllServices() {
        if (pingSender != null) {
            pingSender.stop();
            pingSender = null;
        }

        if (listenerThread != null) {
            listenerThread.interrupt();
            listenerThread = null;
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                LOGGER.warning("Error closing connection: " + e.getMessage());
            }
            connection = null;
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    public String getPlayerId() {
        return null;
    }


    public void stop() {
        // Add necessary logic to stop the network handler
        if (pingSender != null) {
            pingSender.stop();
            pingSender = null;
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (IOException e) {
                LOGGER.severe("Error closing connection: " + e.getMessage());
            }
        }
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
        isConnected = false;
    }

    public void start(String connectionID, int port) {
        // Add necessary logic to start the network handler
        try {
            connection = Connection.createClientSide(connectionID, port);
            startPingMonitoring();
            startListening();
            isConnected = true;
            LOGGER.info("Started network handler with connection ID: " + connectionID);
        } catch (Exception e) {
            LOGGER.severe("Failed to start network handler: " + e.getMessage());
            handleConnectionFailure(e);
        }
    }
}
