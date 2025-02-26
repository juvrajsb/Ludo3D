package ludo.server.networking;

import ludo.core.events.Event;
import ludo.core.network.*;
import ludo.server.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ServerNetworkHandler {
    private static final Logger LOGGER = Logger.getLogger(ServerNetworkHandler.class.getName());
    private static final int MAX_CLIENTS = 4;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 500; // 0.5 seconds

    private MessageListener messageListener;

    // Use concurrent collections for thread safety
    private final Map<String, Connection> clients = new ConcurrentHashMap<>();
    private final Map<String, Queue<NetworkMessage>> outgoingQueues = new ConcurrentHashMap<>();
    private final Map<String, Set<Integer>> processedMessageIds = new ConcurrentHashMap<>();

    private final ServerSocket serverSocket;
    private volatile boolean running;
    private final Server server;

    // For scheduled tasks like message retries
    private final ScheduledExecutorService scheduler;

    public ServerNetworkHandler(Server server, ServerSocket serverSocket) {
        this.server = server;
        this.serverSocket = serverSocket;
        this.scheduler = Executors.newScheduledThreadPool(2);
    }

    public void start() {
        if (running) {
            return;
        }

        try {
            running = true;
            // Start message queue processor
            startOutgoingQueueProcessor();
        } catch (Exception e) {
            LOGGER.severe("Failed to start server: " + e.getMessage());
            throw new RuntimeException("Server startup failed", e);
        }
    }

    public void stop() {
        running = false;

        // Shutdown scheduler
        scheduler.shutdownNow();

        try {
            // Close all client connections
            for (Connection client : clients.values()) {
                try {
                    client.close();
                } catch (IOException e) {
                    LOGGER.warning("Error closing client connection: " + e.getMessage());
                }
            }
            clients.clear();
            outgoingQueues.clear();
            processedMessageIds.clear();

            if (!serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception e) {
            LOGGER.severe("Error stopping server: " + e.getMessage());
        }
    }

    private void startOutgoingQueueProcessor() {
        Runnable processor = () -> {
            for (Map.Entry<String, Queue<NetworkMessage>> entry : outgoingQueues.entrySet()) {
                String clientId = entry.getKey();
                Queue<NetworkMessage> queue = entry.getValue();

                Connection client = clients.get(clientId);
                if (client != null && !client.isClosed()) {
                    NetworkMessage message = queue.poll();
                    if (message != null) {
                        try {
                            client.send(message);
                        } catch (IOException e) {
                            LOGGER.warning("Failed to send queued message to client " + clientId);
                            // Re-queue the message for retry
                            queue.add(message);
                        }
                    }
                }
            }
        };

        scheduler.scheduleAtFixedRate(processor, 100, 100, TimeUnit.MILLISECONDS);
    }

    public void addClient(Connection connection) throws IOException {
        String clientId = connection.getConnectionID();

        if (clients.size() >= MAX_CLIENTS) {
            throw new IOException("Maximum clients reached");
        }

        // Register client
        clients.put(clientId, connection);
        // Create message queue for this client
        outgoingQueues.put(clientId, new ConcurrentLinkedQueue<>());
        // Create set for tracking processed message IDs
        processedMessageIds.put(clientId, Collections.newSetFromMap(new ConcurrentHashMap<>()));

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
                    // Process message only if it's not a duplicate
                    if (message instanceof Event event) {
                        String clientId = connection.getConnectionID();
                        int messageId = event.getMessageId();

                        // Set connection on the event
                        event.setConnection(connection);

                        // Check for duplicates
                        Set<Integer> processed = processedMessageIds.get(clientId);
                        if (processed != null) {
                            if (processed.contains(messageId)) {
                                LOGGER.info("Skipping duplicate message ID: " + messageId);
                                continue;
                            }

                            // Mark as processed
                            processed.add(messageId);

                            // Clean up old message IDs periodically
                            if (processed.size() > 1000) {
                                scheduler.schedule(() -> cleanupOldMessageIds(clientId), 5, TimeUnit.SECONDS);
                            }
                        }

                        // Process non-duplicate message
                        if (messageListener != null) {
                            messageListener.onMessageReceived(event);
                        }
                    } else {
                        // Non-event messages don't have IDs, so just process them
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

    private void cleanupOldMessageIds(String clientId) {
        // In a real implementation, you'd want to keep a timestamp with each ID
        // and remove the oldest ones. For simplicity, we're just clearing all.
        Set<Integer> processed = processedMessageIds.get(clientId);
        if (processed != null) {
            processed.clear();
        }
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

            // Cleanup resources
            outgoingQueues.remove(clientId);
            processedMessageIds.remove(clientId);

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

    public void broadcast(NetworkMessage message) {
        for (String clientId : clients.keySet()) {
            sendToClient(clientId, message);
        }
    }

    public void sendToClient(String clientId, NetworkMessage message) {
        Connection client = clients.get(clientId);
        Queue<NetworkMessage> queue = outgoingQueues.get(clientId);

        if (client != null && queue != null) {
            // Add message to outgoing queue for reliable delivery
            queue.add(message);
        }
    }

    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    public boolean hasClient(String clientId) {
        return clients.containsKey(clientId);
    }

    public List<Connection> getActiveConnections() {
        return new ArrayList<>(clients.values());
    }
}
