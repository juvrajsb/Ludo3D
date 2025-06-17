package ludo.server.networking;

import ludo.core.events.Event;
import ludo.core.network.Connection;

import java.io.IOException;
import java.util.List;

/**
 * Class that allows to send an Event through a Connection in various ways
 */
public class EventTransmitter {
    private final List<Connection> connections;

    public EventTransmitter(List<Connection> connections) {
        this.connections = connections;
    }

    public void broadcast(Event event) {
        connections.forEach(connection -> {
            try {
                connection.send(event);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
