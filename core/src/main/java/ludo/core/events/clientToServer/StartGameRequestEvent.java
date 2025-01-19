package ludo.core.events.clientToServer;

import ludo.core.events.Event;

public class StartGameRequestEvent extends Event {
    public StartGameRequestEvent() {
        super("START_GAME_REQUEST");
    }

    @Override
    public void process() {
        // Will be processed by server handler
    }
}
