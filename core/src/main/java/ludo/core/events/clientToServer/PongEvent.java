package ludo.core.events.clientToServer;

import ludo.core.events.Event;

public class PongEvent extends Event {
    private static final long serialVersionUID = 1L;

    public PongEvent() {
        super("PONG");
    }

    @Override
    public void process() {
        // Will be processed by server handler
    }
}
