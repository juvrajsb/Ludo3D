package ludo.server.networking;

import ludo.core.network.Connection;
import ludo.core.network.NetworkMessage;
import ludo.core.events.Event;
import java.io.IOException;
import java.util.Queue;
import java.util.logging.Logger;

public class EventListener implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(EventListener.class.getName());
    private final Connection listeningConnection;
    private final Queue<Event> eventsQueue;
    private final boolean usedByServer;
    private volatile boolean running;

    public EventListener(Connection connection, Queue<Event> eventsQueue, boolean usedByServer) {
        this.listeningConnection = connection;
        this.eventsQueue = eventsQueue;
        this.usedByServer = usedByServer;
        this.running = true;
    }

    @Override
    public void run() {
        while (running) {
            try {
                NetworkMessage message = listeningConnection.receive();
                if (!(message instanceof Event)) {
                    LOGGER.warning("Received non-event message: " + message.getType());
                    continue;
                }

                Event event = (Event) message;
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
                    ConnectionManager.getInstance().handleDisconnection(
                        listeningConnection.getConnectionID()
                    );
                }
                break;
            } catch (ClassNotFoundException e) {
                LOGGER.severe("Error deserializing message: " + e.getMessage());
                break;
            }
        }
    }

    public void stop() {
        running = false;
    }
}
