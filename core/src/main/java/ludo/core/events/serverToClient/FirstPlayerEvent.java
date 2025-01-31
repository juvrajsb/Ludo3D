package ludo.core.events.serverToClient;

import ludo.core.events.Event;

public class FirstPlayerEvent extends Event {
    private static final long serialVersionUID = 1L;

    public FirstPlayerEvent() {
        super("FIRST_PLAYER");
    }

    @Override
    public void process() {
        // Will be processed by client handler
    }
}
