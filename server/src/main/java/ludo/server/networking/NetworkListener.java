package ludo.server.networking;

import ludo.core.network.Connection;
import ludo.server.Server;
import ludo.core.events.Event;

import java.io.IOException;
import java.net.Socket;
import java.util.Queue;
import java.util.Timer;

import static ludo.server.Server.LOGGER;

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
        while (server.isRunning()) {
            try {
                Socket clientSocket = server.getWelcomeSocket().accept();
                LOGGER.info("New client socket accepted");

                try {
                    // Create and initialize connection
                    Connection connection = Connection.createServerSide(clientSocket);

                    // Register client through single path
                    server.getNetworkHandler().addClient(connection);

                } catch (Exception e) {
                    LOGGER.severe("Failed to initialize client: " + e.getMessage());
                    try {
                        clientSocket.close();
                    } catch (IOException ignored) {}
                }

            } catch (IOException e) {
                if (server.isRunning()) {
                    LOGGER.severe("Error accepting client: " + e.getMessage());
                }
                break;
            }
        }
    }

//    private void startPingRoutine(Connection clientConnection) {//TODO check usage not used currently
//        ServerPingSender pingSender = new ServerPingSender(clientConnection);
//        clientConnection.setPingSender(pingSender);
//        pingSender.start();
//    }
//
//    private static Thread getClientDedicatedThread(Connection clientConnection, Queue<Event> eventsQueue) {//TODO check usage not used currently
//        Thread clientDedicatedThread;
//        clientDedicatedThread = new Thread(
//                new EventListener(clientConnection, eventsQueue, true),
//                "EventListener"
//        );
//        clientDedicatedThread.start();
//        return clientDedicatedThread;
//    }
}
