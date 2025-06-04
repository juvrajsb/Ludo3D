package ludo.client.networking;

import ludo.core.network.*;
import ludo.core.events.Event;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import ludo.core.events.serverToClient.PingEvent;
import ludo.core.events.clientToServer.PongEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * This class is responsible for handling the client network.
 */
public class ClientNetworkHandler implements NetworkHandler {
    private static final Logger LOGGER = Logger.getLogger(ClientNetworkHandler.class.getName());
    private static final int BATCH_SIZE = 10;
    private static final long BATCH_TIMEOUT_MS = 20;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 100;

    private Connection connection;
    private MessageListener listener;
    private final AtomicBoolean isConnected;
    private Thread listenerThread;
    private volatile boolean running;
    private volatile ScheduledExecutorService scheduler;
    private final Object schedulerLock = new Object();

    private final BlockingQueue<NetworkMessage> outgoingQueue;
    private final List<NetworkMessage> messageBatch;
    private long lastBatchTime;

    private final Object disconnectLock = new Object();
    private volatile boolean disconnecting = false;

    public ClientNetworkHandler() {
        this.isConnected = new AtomicBoolean(false);
        this.outgoingQueue = new LinkedBlockingQueue<>();
        this.messageBatch = new ArrayList<>(BATCH_SIZE);
        this.lastBatchTime = System.currentTimeMillis();
    }

    //for testing
    public String getPlayerId() {
        return null;
    }

    @Override
    public synchronized void connect(String host, int port) {
        if (isConnected.get()) {
            LOGGER.warning("Already connected");
            return;
        }

        try {
            // Ensure clean state before connecting
            cleanupResources();

            connection = Connection.createClientSide(host, port);
            startListening();
            isConnected.set(true);
            running = true;
            LOGGER.info("Connected to server at " + host + ":" + port);

            startOutgoingQueueProcessor();
        } catch (Exception e) {
            LOGGER.severe("Failed to connect: " + e.getMessage());
            cleanupResources();
            throw new RuntimeException("Connection failed", e);
        }
    }

    @Override
    public synchronized void disconnect() {
        synchronized (disconnectLock) {
            if (disconnecting) {
                return; // Already disconnecting
            }
            disconnecting = true;
        }

        LOGGER.info("Disconnecting from server...");
        running = false;
        cleanupResources();
        LOGGER.info("Disconnected from server");

        synchronized (disconnectLock) {
            disconnecting = false;
        }
    }

    private void cleanupResources() {
        synchronized (disconnectLock) {
            // Stop the outgoing queue processor
            stopOutgoingQueueProcessor();

            // Close the connection
            if (connection != null) {
                try {
                    connection.close();
                } catch (IOException e) {
                    LOGGER.warning("Error closing connection: " + e.getMessage());
                }
                connection = null;
            }

            // Interrupt the listener thread if it exists
            if (listenerThread != null) {
                listenerThread.interrupt();
                listenerThread = null;
            }

            // Clear queues
            outgoingQueue.clear();
            messageBatch.clear();

            isConnected.set(false);
        }
    }

    private void stopOutgoingQueueProcessor() {
        synchronized (schedulerLock) {
            if (scheduler != null) {
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                scheduler = null;
            }
        }
    }

    private void startListening() {
        listenerThread = new Thread(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    NetworkMessage message = connection.receive();
                    if (message != null) {
                        handleMessage(message);
                    }
                } catch (IOException e) {
                    if (running) {
                        if (e.getMessage() == null && connection.isClosed()) {
                            // This is an expected error during graceful disconnection
                            LOGGER.info("Connection closed during graceful disconnection");
                            break;
                        }
                        if (!disconnecting) {
                            LOGGER.severe("Connection lost: " + e.getMessage());
                            handleConnectionError(e);
                        }
                        break;
                    }
                } catch (Exception e) {
                    if (!disconnecting) {
                        LOGGER.severe("Error processing message: " + e.getMessage());
                    }
                }
            }
        }, "NetworkListener");
        listenerThread.start();
    }

    private void startOutgoingQueueProcessor() {
        synchronized (schedulerLock) {
            if (scheduler != null) {
                stopOutgoingQueueProcessor();
            }
            scheduler = Executors.newSingleThreadScheduledExecutor();

            Runnable processor = () -> {
                try {
                    // Try to get a message with a short timeout
                    NetworkMessage message = outgoingQueue.poll(BATCH_TIMEOUT_MS, TimeUnit.MILLISECONDS);

                    if (message != null && isConnected()) {
                        messageBatch.add(message);

                        // Send batch if we have enough messages or enough time has passed
                        if (messageBatch.size() >= BATCH_SIZE ||
                            (System.currentTimeMillis() - lastBatchTime) >= BATCH_TIMEOUT_MS) {
                            sendBatch();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            };
            scheduler.scheduleAtFixedRate(processor, 0, 5, TimeUnit.MILLISECONDS);
        }
    }

    private void sendBatch() {
        if (messageBatch.isEmpty()) return;

        long startTime = System.nanoTime();
        int retryCount = 0;
        boolean success = false;

        while (!success && retryCount < MAX_RETRIES) {
            try {
                // Group messages by type for more efficient processing
                Map<String, List<NetworkMessage>> messagesByType = messageBatch.stream()
                    .collect(Collectors.groupingBy(NetworkMessage::getType));

                // Send each group of messages
                for (List<NetworkMessage> messages : messagesByType.values()) {
                    for (NetworkMessage message : messages) {
                        if (!(message instanceof PingEvent)) {
                            sendMessageDirect(message);
                        }
                    }
                }

                long endTime = System.nanoTime();
//                LOGGER.info("Batch send time: " + (endTime - startTime) / 1_000_000.0 +
//                           "ms for " + messageBatch.size() + " messages");
                success = true;
            } catch (Exception e) {
                retryCount++;
                if (retryCount < MAX_RETRIES) {
                    LOGGER.warning("Failed to send message batch (attempt " + retryCount +
                                 " of " + MAX_RETRIES + "): " + e.getMessage());
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    LOGGER.severe("Failed to send message batch after " + MAX_RETRIES +
                                " attempts: " + e.getMessage());
                    // Re-queue failed messages
                    outgoingQueue.addAll(messageBatch);
                }
            }
        }

        messageBatch.clear();
        lastBatchTime = System.currentTimeMillis();
    }

    private void handleMessage(NetworkMessage message) {
        if (message instanceof PingEvent) {
            handlePing();
            return;
        }

        if (message instanceof Event event) {
            LOGGER.info("Received event: " + event.getType());

            if (event.getType().equals("DISCONNECTION_ACKNOWLEDGED")) {
                LOGGER.info("Received disconnection acknowledgment from server");
                if (listener != null) {
                    listener.onMessageReceived(event);
                }
                return;
            }

            if (listener != null) {
                listener.onMessageReceived(message); //TODO check if event instead
            }
        }
    }

    private void handlePing() {
        try {
            sendMessage(new PongEvent());
        } catch (Exception e) {
            LOGGER.warning("Failed to send pong: " + e.getMessage());
            handleConnectionError(e);
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
        if (!isConnected.get()) {
            LOGGER.warning("Cannot send message, not connected to server");
            return;
        }

        // For ping messages, send immediately
        if (message instanceof PingEvent) {
            try {
                sendMessageDirect(message);
            } catch (Exception e) {
                LOGGER.warning("Failed to send ping: " + e.getMessage());
            }
            return;
        }

        // For other messages, add to queue
        try {
            outgoingQueue.offer(message, 100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warning("Interrupted while queueing message");
        }
    }

    private void sendMessageDirect(NetworkMessage message) {
        if (!isConnected.get() || connection == null) {
            throw new IllegalStateException("Not connected to server");
        }

        try {
            connection.send(message);
        } catch (IOException e){
            LOGGER.warning("Failed to send message: " + e.getMessage());
            handleConnectionError(e);
            throw new RuntimeException("Failed to send message", e);
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
