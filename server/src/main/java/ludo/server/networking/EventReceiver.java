package ludo.server.networking;

import ludo.core.events.Event;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Class that receives the Events from the socket through a Queue
 * and calls their associated handlers.
 *
 * @see EventListener
 */
public class EventReceiver implements Runnable {
    private final Queue<Event> eventsQueue;
    private boolean stop = false;

    public EventReceiver() {
        this.eventsQueue = new LinkedList<>();
    }

    public Queue<Event> getEventsQueue() {
        return eventsQueue;
    }

    /**
     * Method that allows the reception of events. The thread sleeps until an Event arrives: when an Event arrives, the
     * EventReceiver is woken up and can receive the event by reading it from the {@code eventsQueue}.
     */
    @Override
    public void run() {
        while(true) {
            stop = false;
            while (eventsQueue.isEmpty()) {
                if (stop) return;
                try {
                    synchronized (EventReceiver.class) {
                        EventReceiver.class.wait();
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            Event receivedEvent;
            // event received
            synchronized (eventsQueue) {
                receivedEvent = eventsQueue.remove();
            }

            receivedEvent.process();
        }
    }

    /**
     * Method used to stop the EventReceiver
     */
    public synchronized void stop() { //TODO check usage not used currently
        notifyAll();
        this.stop = true;
        synchronized (EventReceiver.class) {
            EventReceiver.class.notifyAll();
        }
    }
}
