package ludo.core.events.clientToServer;

import ludo.core.events.Event;

public class ClientDisconnectedEvent extends Event {
    private static final long serialVersionUID = 1L;
    private final String clientId;

    public ClientDisconnectedEvent(String clientId) {
        super("CLIENT_DISCONNECTED");
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }

    @Override
    public void process() {
        // Will be processed by server handler
    }
}
