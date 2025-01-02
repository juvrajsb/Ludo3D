package ludo.server.networking;

import ludo.server.events.Event;

import java.io.IOException;
import java.util.Queue;

/**
 * Runnable class that listens for a connection and adds every Event that arrives to the passedQueue
 */
public class EventListener implements Runnable {
    /**
     * Connection that the class listens to
     */
    private final Connection listeningConnection;
    private final Queue<Event> eventsQueue;
    private final boolean usedByServer;

    public EventListener(Connection connection, Queue<Event> eventsQueue, boolean usedByServer) {
        this.listeningConnection = connection;
        this.eventsQueue = eventsQueue;
        this.usedByServer = usedByServer;
    }


    @Override
    public void run() {
        Event receivedEvent;
        while (true) {
            try {
                receivedEvent = listeningConnection.receive();
            } catch (IOException e) {
                if (usedByServer) {
                    ConnectionsController.getInstance().signalClientDisconnection(listeningConnection);
                }
                break;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            receivedEvent.setConnection(listeningConnection);

            synchronized (eventsQueue) {
                eventsQueue.add(receivedEvent);
            }
            synchronized (EventReceiver.class) {
                EventReceiver.class.notifyAll();
            }
        }
    }
}
