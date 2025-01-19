package ludo.core.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class Connection {
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private String connectionId;
    private PingSender pingSender;
    private final AtomicInteger pingFailureCount;

    private static final int MAX_PING_FAILURES = 5;
    private static final int CONNECTION_TIMEOUT = 5000; // 5 seconds

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
        return new Connection(socket, "client");
    }

    private static String generateConnectionId() {
        return "client-" + UUID.randomUUID().toString().substring(0, 8);
    }

    // Private constructor - force use of factory methods
    private Connection(Socket socket, String id) throws IOException {
        this.socket = socket;
        this.connectionId = id;
        // Create output stream first to avoid potential deadlock
        this.out = new ObjectOutputStream(socket.getOutputStream());
        out.flush(); // Ensure header is written
        this.in = new ObjectInputStream(socket.getInputStream());
        this.pingFailureCount = new AtomicInteger(0);
    }

    public void send(NetworkMessage message) throws IOException {
        synchronized(out) {
            out.writeObject(message.serialize());
            out.flush();
            out.reset();  // Reset object cache to prevent memory leaks
        }
    }

    public NetworkMessage receive() throws IOException, ClassNotFoundException {
        byte[] data = (byte[])in.readObject();
        return NetworkMessage.deserialize(data);
    }

    public String getConnectionID() {
        return connectionId;
    }

    public void setConnectionID(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getHost() {
        return socket.getInetAddress().getHostAddress();
    }

    public int getPort() {
        return socket.getPort();
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
        if (pingSender != null) {
            pingSender.stop();
        }
        out.close();
        in.close();
        socket.close();
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }
}
