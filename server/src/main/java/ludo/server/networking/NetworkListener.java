package ludo.server.networking;

import ludo.core.network.Connection;
import ludo.core.network.PingSender;
import ludo.server.Server;
import ludo.core.events.Event;

import java.io.IOException;
import java.net.Socket;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Class that is used by the Server to listen for incoming connections
 */
public class NetworkListener implements Runnable {

    private final Server server;
    public NetworkListener(Server server) {
        this.server = server;
    }

    /**
     * Method that listens for entering connections and for each new connected client adds a new Connection and
     * EventListener thread to the controller. It also starts a Timer that every PING_DELAY ms sends an event to check if
     * this connection is still active
     * @see Timer
     */
    @Override
    public void run() {
        Queue<Event> eventsQueue = server.getEventReceiver().getEventsQueue();
        Socket clientSocket;
        Connection clientConnection = null;
        Thread clientDedicatedThread;

        while (true) {
            try {
                clientSocket = server.getWelcomeSocket().accept();
                Server.LOGGER.info("New Client connected!");
            } catch (IOException e) {
                Server.LOGGER.info("Welcome socket was closed");
                break;
            }

            try {
                clientConnection = Connection.createServerSide(clientSocket);
            } catch (IOException e) {
                Server.LOGGER.severe("Connection with client failed: " + e.getMessage());
                try {
                    clientSocket.close();  // Clean up failed socket
                } catch (IOException ignored) {}
                continue;  // Continue to next iteration to accept new connections
            }

            clientDedicatedThread = getClientDedicatedThread(clientConnection, eventsQueue);
            Server.LOGGER.info("Client dedicated thread started");
            server.addClient(clientConnection, new Thread(clientDedicatedThread));

            startPingRoutine(clientConnection);
        }
    }

    private static void startPingRoutine(Connection clientConnection) {
        ServerPingSender pingSender = new ServerPingSender(clientConnection);
        clientConnection.setPingSender(pingSender);
        pingSender.start();
    }

    private static Thread getClientDedicatedThread(Connection clientConnection, Queue<Event> eventsQueue) {
        Thread clientDedicatedThread;
        clientDedicatedThread = new Thread(
                new EventListener(clientConnection, eventsQueue, true),
                "EventListener"
        );
        clientDedicatedThread.start();
        return clientDedicatedThread;
    }
}
