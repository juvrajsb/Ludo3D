package ludo.core.events.clientToServer;

import ludo.core.events.Event;

public class StartGameRequestEvent extends Event {
    private static final long serialVersionUID = 1L;
    private final boolean enableBots;
    private final boolean loadSavedGame;

    public StartGameRequestEvent(boolean enableBots, boolean loadSavedGame) {
        super("START_GAME_REQUEST");
        this.enableBots = enableBots;
        this.loadSavedGame = loadSavedGame;
    }

    public StartGameRequestEvent(boolean enableBots) {
        this(enableBots, false);
    }

    public boolean isBotsEnabled() {
        return enableBots;
    }

    public boolean isLoadSavedGame() {
        return loadSavedGame;
    }

    @Override
    public void process() {
        // Will be processed by server handler
    }
}
