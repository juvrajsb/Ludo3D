package ludo.server.networking;

import ludo.server.events.Event;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.TimerTask;

/**
 * Class that handles a Connection between two hosts at socket level
 */
public class Connection {
    private final Socket foreignHostSocket;
    private String connectionID;
    private final ObjectInputStream inputStream;
    private final ObjectOutputStream outputStream;
    private static final int TIMEOUT = 2000;
    private final int PING_FAILURE_THRESHOLD = 5;
    private boolean ready = false;
    private int currentPingFailure = PING_FAILURE_THRESHOLD;
    private TimerTask pingSender;


    /**
     * Used to create a new Client side connection
     * @param address address to connect to
     * @param port port to connect to
     */
    public Connection(String address, int port) throws IOException {
        this.foreignHostSocket = new Socket();
        foreignHostSocket.connect(new InetSocketAddress(address, port), TIMEOUT);
        outputStream = new ObjectOutputStream(foreignHostSocket.getOutputStream());
        inputStream = new ObjectInputStream(foreignHostSocket.getInputStream());
        connectionID = "controller";
        ready = true;
    }

    /**
     * Used to create a new Server side connection
     */
    public Connection(Socket socket) throws IOException {
        this.foreignHostSocket = socket;
        outputStream = new ObjectOutputStream(foreignHostSocket.getOutputStream());
        inputStream = new ObjectInputStream(foreignHostSocket.getInputStream());
        ready = true;
    }

    /**
     * Method that allows to send an Event through this Connection socket
     * @param event an Event object to send
     */
    public void send(Event event) throws IOException {
        if(!ready) return;
        synchronized (outputStream) {
            outputStream.writeObject(event);
            outputStream.flush();
            outputStream.reset();
        }
    }

    /**
     * Method that allows to receive an Event from the socket
     * @return an Event received through the Socket
     */
    public Event receive() throws IOException, ClassNotFoundException {
        return (Event) inputStream.readObject();
    }

    public void close() throws IOException {
        synchronized (outputStream) {
            outputStream.close();
        }
        inputStream.close();
        foreignHostSocket.close();
        ready = false;
    }

    public void decrementPingFailure() {
        currentPingFailure--;
    }

    public void resetPingFailure() {
        currentPingFailure = PING_FAILURE_THRESHOLD;
    }

    public boolean isFailed() {
        return currentPingFailure == 0;
    }

    public String getConnectionID() {
        return connectionID;
    }

    public void setConnectionID(String connectionID) {
        this.connectionID = connectionID;
    }

    public TimerTask getPingSender() {
        return pingSender;
    }

    public void setPingSender(TimerTask pingSender) {
        this.pingSender = pingSender;
    }
}
