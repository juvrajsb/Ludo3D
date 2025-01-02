package ludo.server.events.clientToServer;

import ludo.server.events.Event;

/**
 * Events that signals a client disconnection.
 */
public class ClientDisconnectedEvent extends Event {
    public ClientDisconnectedEvent() {
        ID = "CLIENT_DISCONNECTED";
    }

    @Override
    public void callHandler() {
        ConnectionsController.getInstance().handle(this);
    }
}
