package ludo.core.events.serverToClient;

import ludo.core.events.Event;

public class CanRollAgainEvent extends Event {
    private static final long serialVersionUID = 1L;

    public CanRollAgainEvent() {
        super("CAN_ROLL_AGAIN");
    }

    @Override
    public void process() {
        // Will be processed by client handler
    }
}
