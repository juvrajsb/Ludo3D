package ludo.core.network;

import ludo.core.events.Event;
import ludo.core.events.serverToClient.PingEvent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

public class Connection {
    private static final Logger LOGGER = Logger.getLogger(Connection.class.getName());

    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private String connectionId;
    private PingSender pingSender;
    private final AtomicInteger pingFailureCount;
    private final AtomicInteger messageIdCounter;
    private volatile boolean closed = false;
    private volatile boolean gracefullyClosed = false;
    private ConnectionState currentState;

    private static final int MAX_PING_FAILURES = 5;
    private static final int CONNECTION_TIMEOUT = 5000; // 5 seconds
    private static final int SOCKET_READ_TIMEOUT = 15000; // 15 seconds

    // Connection states
    private interface ConnectionState {
        void connect() throws IOException;
        void disconnect() throws IOException;
        void send(NetworkMessage message) throws IOException;
        NetworkMessage receive() throws IOException;
    }

    private class ConnectedState implements ConnectionState {
        @Override
        public void connect() throws IOException {
            throw new IOException("Already connected");
        }

        @Override
        public void disconnect() throws IOException {
            close();
            currentState = new DisconnectedState();
        }

        @Override
        public void send(NetworkMessage message) throws IOException {
            if (closed || !socket.isConnected() || socket.isClosed()) {
                throw new IOException("Connection is closed");
            }

            long startTime = System.nanoTime();

            if (message instanceof Event) {
                ((Event) message).setMessageId(messageIdCounter.incrementAndGet());
            }

            synchronized(out) {
                try {
                    out.writeObject(message);
                    out.flush();
                    out.reset();

                    long endTime = System.nanoTime();
                    if (!(message instanceof PingEvent)) {
//                        LOGGER.info("Message send time: " + (endTime - startTime) / 1_000_000.0 + "ms for " + message.getType());
                    }
                } catch (IOException e) {
                    LOGGER.severe("Error sending message on connection " + connectionId + ": " + e.getMessage());
                    closed = true;
                    throw e;
                }
            }
        }

        @Override
        public NetworkMessage receive() throws IOException {
            if (closed || !socket.isConnected() || socket.isClosed()) {
                throw new IOException("Connection is closed");
            }

            long startTime = System.nanoTime();

            try {
                NetworkMessage message = (NetworkMessage) in.readObject();
                long endTime = System.nanoTime();

                if (!(message instanceof PingEvent)) {
                }
                return message;
            } catch (SocketTimeoutException e) {
                LOGGER.fine("Socket read timeout on connection " + connectionId + " - normal behavior");
                throw e;
            } catch (IOException | ClassNotFoundException e) {
                LOGGER.severe("Error receiving message on connection " + connectionId + ": " + e.getMessage());
                closed = true;
                throw new IOException(e);
            }
        }
    }

    private class DisconnectedState implements ConnectionState {
        @Override
        public void connect() throws IOException {
            throw new IOException("Cannot connect in disconnected state");
        }

        @Override
        public void disconnect() throws IOException {
            // Already disconnected
        }

        @Override
        public void send(NetworkMessage message) throws IOException {
            throw new IOException("Cannot send message in disconnected state");
        }

        @Override
        public NetworkMessage receive() throws IOException {
            throw new IOException("Cannot receive message in disconnected state");
        }
    }

    /**
     * Creates a server-side connection from an existing socket
     */
    public static Connection createServerSide(Socket socket) throws IOException {
        try {
            // First read the client's ID from the socket
            ObjectInputStream tempIn = new ObjectInputStream(socket.getInputStream());
            String clientId = (String) tempIn.readObject();
            return new Connection(socket, clientId);
        } catch (ClassNotFoundException e) {
            throw new IOException("Failed to read client ID: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a client-side connection to a specified host/port
     */
    public static Connection createClientSide(String host, int port) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT);
        socket.setSoTimeout(SOCKET_READ_TIMEOUT);

        // Generate a unique ID for this client
        String clientId = generateConnectionId();

        // Send the ID to the server before creating the connection
        ObjectOutputStream tempOut = new ObjectOutputStream(socket.getOutputStream());
        tempOut.writeObject(clientId);
        tempOut.flush();

        return new Connection(socket, clientId);
    }

    private static String generateConnectionId() {
        return "client-" + UUID.randomUUID().toString().substring(0, 8);
    }

    // Private constructor - force use of factory methods
    private Connection(Socket socket, String id) throws IOException {
        this.socket = socket;
        this.connectionId = id;
        this.messageIdCounter = new AtomicInteger(0);
        this.pingFailureCount = new AtomicInteger(0);
        this.currentState = new DisconnectedState();

        // Set socket timeout for read operations
        socket.setSoTimeout(SOCKET_READ_TIMEOUT);

        // Create output stream first to avoid potential deadlock
        this.out = new ObjectOutputStream(socket.getOutputStream());
        out.flush(); // Ensure header is written
        this.in = new ObjectInputStream(socket.getInputStream());

        // Transition to connected state
        this.currentState = new ConnectedState();
    }

    public void send(NetworkMessage message) throws IOException {
        currentState.send(message);
    }

    public NetworkMessage receive() throws IOException {
        return currentState.receive();
    }

    public String getConnectionID() {
        return connectionId;
    }

    public void setConnectionID(String connectionId) {
        this.connectionId = connectionId;
    }

    public boolean isFailed() {
        return pingFailureCount.get() >= MAX_PING_FAILURES;
    }

    public void decrementPingFailure() {
        int count = pingFailureCount.get();
        if (count > 0) {
            pingFailureCount.decrementAndGet();
        }
    }

    public void incrementPingFailure() {
        pingFailureCount.incrementAndGet();
    }

    public void resetPingFailure() {
        pingFailureCount.set(0);
    }

    public void setPingSender(PingSender pingSender) {
        this.pingSender = pingSender;
    }

    public PingSender getPingSender() {
        return pingSender;
    }

    public void close() throws IOException {
        synchronized(this) {
            if (!closed) {
                closed = true;
                gracefullyClosed = true;

                if (pingSender != null) {
                    LOGGER.info("Stopping ping sender for " + connectionId);
                    pingSender.stop();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        LOGGER.warning("Interrupted while waiting for ping sender to stop: " + e.getMessage());
                    }
                    LOGGER.info("Ping sender stopped for " + connectionId);
                }

                try {
                    in.close();
                } catch (IOException e) {
                    LOGGER.warning("Error closing input stream for " + connectionId + ": " + e.getMessage());
                }

                try {
                    out.close();
                } catch (IOException e) {
                    LOGGER.warning("Error closing output stream for " + connectionId + ": " + e.getMessage());
                }

                try {
                    socket.close();
                    LOGGER.info("Successfully closed connection for " + connectionId);
                } catch (IOException e) {
                    LOGGER.severe("Error closing socket for " + connectionId + ": " + e.getMessage());
                    throw e;
                }
            } else {
                LOGGER.info("Connection " + connectionId + " already closed");
            }
        }
    }

    public boolean isClosed() {
        return closed || socket.isClosed() || !socket.isConnected();
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }

    public void setSoTimeout(int timeout) throws IOException {
        socket.setSoTimeout(timeout);
    }

    public boolean isGracefullyClosed() {
        return gracefullyClosed;
    }
}
