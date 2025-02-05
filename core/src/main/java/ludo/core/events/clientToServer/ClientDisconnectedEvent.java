package ludo.core.events.clientToServer;

import ludo.core.events.Event;

public class ClientDisconnectedEvent extends Event {
    private static final long serialVersionUID = 1L;

    public ClientDisconnectedEvent() {
        super("CLIENT_DISCONNECTED");
    }

    @Override
    public void process() {
        // Will be processed by server handler
    }
}
