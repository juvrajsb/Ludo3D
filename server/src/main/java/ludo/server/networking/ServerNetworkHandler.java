package ludo.server.networking;

import ludo.core.events.Event;
import ludo.core.network.*;
import ludo.server.Server;
import ludo.core.events.serverToClient.PingEvent;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ServerNetworkHandler {
    private static final Logger LOGGER = Logger.getLogger(ServerNetworkHandler.class.getName());
    private static final int MAX_CLIENTS = 4;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 500; // 0.5 seconds
    private static final int BATCH_SIZE = 10;
    private static final long BATCH_TIMEOUT_MS = 20;
    private static final int MAX_RETRIES = 3;

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

    private final Map<String, List<NetworkMessage>> clientMessageBatches;
    private final Map<String, Long> lastBatchTimes;

    public ServerNetworkHandler(Server server, ServerSocket serverSocket) {
        this.server = server;
        this.serverSocket = serverSocket;
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.clientMessageBatches = new ConcurrentHashMap<>();
        this.lastBatchTimes = new ConcurrentHashMap<>();
        startBatchProcessor();
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
        try {
            connection.setSoTimeout(2000); // 2 second timeout for faster disconnect detection
        } catch (IOException e) {
            LOGGER.warning("Failed to set socket timeout for client " + connection.getConnectionID() + ": " + e.getMessage());
        }
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

                        // Special handling for disconnection events
                        if (event instanceof ludo.core.events.clientToServer.ClientDisconnectedEvent) {
                            LOGGER.info("Received disconnection event from client: " + clientId);
                            
                            try {
                                // Stop ping sender first
                                if (connection.getPingSender() != null) {
                                    connection.getPingSender().stop();
                                }
                                
                                // Process the disconnection event
                                if (messageListener != null) {
                                    messageListener.onMessageReceived(event);
                                }
                                
                                // Clean up after processing
                                handleClientError(connection);
                            } catch (Exception e) {
                                LOGGER.severe("Error handling disconnection for client " + clientId + ": " + e.getMessage());
                                handleClientError(connection);
                            }
                            
                            // Skip normal message processing for disconnection events
                            continue;
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
                } catch (java.net.SocketTimeoutException e) {
                    // This is expected occasionally, just continue
                    continue;
                } catch (Exception e) {
                    if (!connection.isClosed()) {
                        LOGGER.warning("Error handling message for client " + connection.getConnectionID() + ": " + e.getMessage());
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
        LOGGER.info("Client disconnected: " + clientId);

        try {
            // Stop ping sender first
            if (connection.getPingSender() != null) {
                connection.getPingSender().stop();
            }

            // Remove from active clients
            clients.remove(clientId);
            outgoingQueues.remove(clientId);
            processedMessageIds.remove(clientId);

            // Close the connection
            try {
                connection.close();
            } catch (IOException e) {
                LOGGER.warning("Error closing connection for client " + clientId + ": " + e.getMessage());
            }

            // Only send UNEXPECTED_DISCONNECTION if this wasn't a graceful disconnection
            if (!connection.isGracefullyClosed() && messageListener != null) {
                ludo.core.events.serverToClient.UnexceptedDisconnetionEvent event = new ludo.core.events.serverToClient.UnexceptedDisconnetionEvent();
                event.setConnection(connection);
                messageListener.onMessageReceived(event);
            }
        } catch (Exception e) {
            LOGGER.severe("Error handling client disconnection: " + e.getMessage());
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

    public Server getServer() {
        return server;
    }

    private void startBatchProcessor() {
        scheduler.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();
            for (Map.Entry<String, List<NetworkMessage>> entry : clientMessageBatches.entrySet()) {
                String clientId = entry.getKey();
                List<NetworkMessage> batch = entry.getValue();
                
                if (!batch.isEmpty() && 
                    (batch.size() >= BATCH_SIZE || 
                     (currentTime - lastBatchTimes.getOrDefault(clientId, 0L)) >= BATCH_TIMEOUT_MS)) {
                    sendBatch(clientId, batch);
                }
            }
        }, 0, 5, TimeUnit.MILLISECONDS);
    }

    private void sendBatch(String clientId, List<NetworkMessage> batch) {
        if (batch.isEmpty()) return;

        long startTime = System.nanoTime();
        int retryCount = 0;
        boolean success = false;

        while (!success && retryCount < MAX_RETRIES) {
            try {
                // Group messages by type for more efficient processing
                Map<String, List<NetworkMessage>> messagesByType = batch.stream()
                    .collect(Collectors.groupingBy(NetworkMessage::getType));

                // Send each group of messages
                for (List<NetworkMessage> messages : messagesByType.values()) {
                    for (NetworkMessage message : messages) {
                        if (!(message instanceof PingEvent)) {
                            sendMessageToClient(clientId, message);
                        }
                    }
                }

                long endTime = System.nanoTime();
                LOGGER.info("Batch send time: " + (endTime - startTime) / 1_000_000.0 + 
                           "ms for " + batch.size() + " messages to client " + clientId);
                success = true;
            } catch (Exception e) {
                retryCount++;
                if (retryCount < MAX_RETRIES) {
                    LOGGER.warning("Failed to send message batch to client " + clientId + 
                                 " (attempt " + retryCount + " of " + MAX_RETRIES + "): " + e.getMessage());
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    LOGGER.severe("Failed to send message batch to client " + clientId + 
                                " after " + MAX_RETRIES + " attempts: " + e.getMessage());
                }
            }
        }

        batch.clear();
        lastBatchTimes.put(clientId, System.currentTimeMillis());
    }

    public void queueMessage(String clientId, NetworkMessage message) {
        if (message instanceof PingEvent) {
            // Send ping messages immediately
            try {
                sendMessageToClient(clientId, message);
            } catch (Exception e) {
                LOGGER.warning("Failed to send ping to client " + clientId + ": " + e.getMessage());
            }
            return;
        }

        clientMessageBatches.computeIfAbsent(clientId, k -> new ArrayList<>(BATCH_SIZE))
            .add(message);
    }

    private void sendMessageToClient(String clientId, NetworkMessage message) {
        // Implementation depends on how you store client connections
        // This is a placeholder for the actual implementation
        Connection clientConnection = getClientConnection(clientId);
        if (clientConnection != null && !clientConnection.isClosed()) {
            try {
                clientConnection.send(message);
            } catch (IOException e) {
                LOGGER.warning("Failed to send message to client " + clientId + ": " + e.getMessage());
                handleClientDisconnection(clientId);
            }
        }
    }

    private Connection getClientConnection(String clientId) {
        // Implementation depends on how you store client connections
        // This is a placeholder for the actual implementation
        return null;
    }

    private void handleClientDisconnection(String clientId) {
        clientMessageBatches.remove(clientId);
        lastBatchTimes.remove(clientId);
        // Additional disconnection handling logic
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
