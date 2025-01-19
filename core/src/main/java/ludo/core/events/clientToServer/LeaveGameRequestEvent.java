package ludo.core.events.clientToServer;

import ludo.core.events.Event;

public class LeaveGameRequestEvent extends Event {
    public LeaveGameRequestEvent() {
        super("LEAVE_GAME_REQUEST");
    }

    @Override
    public void process() {
        // Will be processed by server handler
    }
}
