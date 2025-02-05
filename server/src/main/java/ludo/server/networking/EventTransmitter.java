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

    /**
     * Sends the event to player identified by passed username
     * @param username player to send the event to
     * @param event BaseEvent object to send
     */
    public void sendTo(String username, Event event) { //TODO check usage not used currently
        for(Connection connection: connections) {
            if(connection.getConnectionID().equals(username)) {
                try {
                    connection.send(event);
                    break;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Sends the event to all players
     * @param event Event object to broadcast
     */
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
