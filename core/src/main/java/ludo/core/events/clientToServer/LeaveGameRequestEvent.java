package ludo.core.events.clientToServer;

import ludo.core.events.Event;

public class LeaveGameRequestEvent extends Event {
    private static final long serialVersionUID = 1L;

    public LeaveGameRequestEvent() {
        super("LEAVE_GAME_REQUEST");
    }

    @Override
    public void process() {
        // Will be processed by server handler
    }
}
