package ludo.core.network;

import ludo.core.events.Event;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

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

    private static final int MAX_PING_FAILURES = 5;
    private static final int CONNECTION_TIMEOUT = 5000; // 5 seconds
    private static final int SOCKET_READ_TIMEOUT = 30000; // 30 seconds

    /**
     * Creates a server-side connection from an existing socket
     */
    public static Connection createServerSide(Socket socket) throws IOException {
        String tempId = generateConnectionId();
        return new Connection(socket, tempId);
    }

    /**
     * Creates a client-side connection to a specified host/port
     */
    public static Connection createClientSide(String host, int port) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT);
        socket.setSoTimeout(SOCKET_READ_TIMEOUT);
        return new Connection(socket, "client");
    }

    private static String generateConnectionId() {
        return "client-" + UUID.randomUUID().toString().substring(0, 8);
    }

    // Private constructor - force use of factory methods
    private Connection(Socket socket, String id) throws IOException {
        this.socket = socket;
        this.connectionId = id;
        this.messageIdCounter = new AtomicInteger(0);

        // Set socket timeout for read operations
        socket.setSoTimeout(SOCKET_READ_TIMEOUT);

        // Create output stream first to avoid potential deadlock
        this.out = new ObjectOutputStream(socket.getOutputStream());
        out.flush(); // Ensure header is written
        this.in = new ObjectInputStream(socket.getInputStream());
        this.pingFailureCount = new AtomicInteger(0);
    }

    public void send(NetworkMessage message) throws IOException {
        if (closed || !socket.isConnected() || socket.isClosed()) {
            throw new IOException("Connection is closed");
        }

        // Add message ID for idempotent processing
        if (message instanceof Event) {
            ((Event) message).setMessageId(messageIdCounter.incrementAndGet());
        }

        synchronized(out) {
            try {
                out.writeObject(message);
                out.flush();
                out.reset();
                LOGGER.fine("Sent message: " + message.getType());
            } catch (IOException e) {
                closed = true;
                LOGGER.warning("Error sending message: " + e.getMessage());
                throw e;
            }
        }
    }

    public NetworkMessage receive() throws IOException, ClassNotFoundException {
        if (closed || !socket.isConnected() || socket.isClosed()) {
            throw new IOException("Connection is closed");
        }

        try {
            return (NetworkMessage) in.readObject();
        } catch (SocketTimeoutException e) {
            // This is not an error, just a timeout on read
            LOGGER.fine("Socket read timeout - normal behavior");
            throw e;
        } catch (IOException e) {
            closed = true;
            LOGGER.warning("Error receiving message: " + e.getMessage());
            throw e;
        }
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
        if (!closed) {
            closed = true;
            if (pingSender != null) {
                pingSender.stop();
            }
            try {
                out.close();
            } catch (IOException e) {
                LOGGER.warning("Error closing output stream: " + e.getMessage());
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    LOGGER.warning("Error closing input stream: " + e.getMessage());
                } finally {
                    socket.close();
                }
            }
        }
    }

    public boolean isClosed() {
        return closed || socket.isClosed() || !socket.isConnected();
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }
}
