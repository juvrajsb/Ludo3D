package ludo.core.events.serverToClient;

import ludo.core.events.Event;

public class DisconnectionAcknowledgedEvent extends Event {
    private static final long serialVersionUID = 1L;

    public DisconnectionAcknowledgedEvent() {
        super("DISCONNECTION_ACKNOWLEDGED");
    }

    @Override
    public void process() {
        // Will be processed by client handler
    }
} 