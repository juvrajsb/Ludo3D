package ludo.server;

import ludo.core.network.Connection;
import ludo.server.networking.EventReceiver;
import ludo.server.networking.EventTransmitter;
import ludo.server.networking.NetworkListener;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


/**
 * Singleton class that contains all fundamental methods to handle a controller
 * TODO: maybe bring back connected clients as List if is not necessary to close the Threads
 */
public class Server {
    /**
     * Singleton controller instance
     */
    private static Server instance = null;
    private final int port;
    private final ServerSocket welcomeSocket;
    private final NetworkListener networkListener;
    /**
     * map between clients and their dedicated thread
     */
    private final Map<Connection, Thread> connectedClients;
    private final EventTransmitter eventTransmitter;
    private final EventReceiver eventReceiver;

    public static final Logger LOGGER = Logger.getLogger(Server.class.getName());
    public static final int DEFAULT_PORT = 12000;

    private Server(int port) {
        connectedClients = new HashMap<>();
        eventReceiver = new EventReceiver();
        eventTransmitter = new EventTransmitter(connectedClients.keySet().stream().toList());
        this.port = port;
        networkListener = new NetworkListener(this);
        try {
            welcomeSocket = new ServerSocket(port);
        } catch (IOException e) {
            Server.LOGGER.severe("Could not instantiate controller on port " + port);
            throw new RuntimeException();
        }
    }

    /**
     * Returns the Singleton instance of this Server
     * @param port port on which the controller will start
     * @return the Singleton controller instance
     */
    public static Server getInstance(int port) {
        if(instance == null)
            instance = new Server(port);
        return instance;
    }

    public static Server getInstance() {
        if(instance == null)
            instance = new Server(Server.DEFAULT_PORT);
        return instance;
    }

    /**
     * Method that starts the controller by starting to listen for incoming clients connections with the NetworkListener
     * and for incoming events with the EventReceiver
     */
    public void start() {
        new Thread(networkListener, "NetworkListener").start();
        new Thread(eventReceiver, "EventReceiver").start();

        try {
            Server.LOGGER.info("Server started on port " + port + ".\nUse the following IP to connect locally: " + InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Stops the controller by closing all connections and by stopping all running threads
     */
    public void stop() {
        try {
            for(Connection connection: connectedClients.keySet()) {
                connection.close();
            }
            for(Thread thread: connectedClients.values()) {
                thread.interrupt();
            }
            eventReceiver.stop();
            welcomeSocket.close();
            Server.LOGGER.info("Server stopped correctly.");
        } catch (IOException e) {
            Server.LOGGER.severe("Server didn't stop correctly. " +
                    "There might still be some active threads or open connections:\n".concat(e.getMessage()));
        }
    }

    public ServerSocket getWelcomeSocket() {
        return welcomeSocket;
    }

    public synchronized void addClient(Connection connection, Thread thread) {
        connectedClients.put(connection, thread);
    }

    public List<Connection> getAllConnections() {
        return connectedClients.keySet().stream().toList();
    }

    public EventReceiver getEventReceiver() {
        return eventReceiver;
    }
    public EventTransmitter getEventTransmitter() {return eventTransmitter;}

    public void removeClient(Connection connection) {
        if(connectedClients.containsKey(connection))
            connectedClients.get(connection).interrupt();
        connectedClients.remove(connection);
    }
}
