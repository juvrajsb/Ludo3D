package ludo.core.events.clientToServer;

import ludo.core.events.Event;

public class StartGameRequestEvent extends Event {
    private static final long serialVersionUID = 1L;
    private final boolean enableBots;

    public StartGameRequestEvent(boolean enableBots) {
        super("START_GAME_REQUEST");
        this.enableBots = enableBots;
    }

    public boolean isBotsEnabled() {
        return enableBots;
    }

    @Override
    public void process() {
        // Will be processed by server handler
    }
}
