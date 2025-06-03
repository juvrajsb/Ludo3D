package ludo.server.networking;

import ludo.core.network.Connection;
import ludo.core.network.NetworkMessage;
import ludo.core.events.Event;
import ludo.server.Server;
import java.io.IOException;
import java.util.Queue;
import java.util.logging.Logger;

public class EventListener implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(EventListener.class.getName());
    private final Connection listeningConnection;
    private final Queue<Event> eventsQueue;
    private final boolean usedByServer;
    private final Server server;
    private volatile boolean running;

    public EventListener(Connection connection, Queue<Event> eventsQueue, boolean usedByServer, Server server) {
        this.listeningConnection = connection;
        this.eventsQueue = eventsQueue;
        this.usedByServer = usedByServer;
        this.server = server;
        this.running = true;
    }

    @Override
    public void run() {
        while (running) {
            try {
                NetworkMessage message = listeningConnection.receive();
                if (!(message instanceof Event event)) {
                    LOGGER.warning("Received non-event message: " + message.getType());
                    continue;
                }

                event.setConnection(listeningConnection);

                synchronized (eventsQueue) {
                    eventsQueue.add(event);
                }
                synchronized (EventReceiver.class) {
                    EventReceiver.class.notifyAll();
                }

            } catch (IOException e) {
                if (usedByServer) {
                    // Handle server-side disconnection
                    ConnectionManager.getInstance(server).handleDisconnection(
                        listeningConnection.getConnectionID()
                    );
                }
                break;
            }
        }
    }

//    public void stop() {//TODO check usage not used currently
//        running = false;
//    }
}
