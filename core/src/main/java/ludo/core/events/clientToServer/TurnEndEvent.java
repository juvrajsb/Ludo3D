package ludo.core.events.clientToServer;

import ludo.core.events.Event;

public class TurnEndEvent extends Event {
    private static final long serialVersionUID = 1L;

    public TurnEndEvent() {
        super("TURN_END");
    }

    @Override
    public void process() {
        // Will be processed by server handler
    }
}
