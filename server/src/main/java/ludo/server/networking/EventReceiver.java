package ludo.server.networking;

import ludo.core.events.Event;

import java.util.LinkedList;
import java.util.Queue;

public class EventReceiver implements Runnable {
    private final Queue<Event> eventsQueue;
    private boolean stop = false;

    public EventReceiver() {
        this.eventsQueue = new LinkedList<>();
    }

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
}
