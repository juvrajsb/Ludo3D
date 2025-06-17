package ludo.core.events.clientToServer;

import ludo.core.events.Event;

public class StartGameRequestEvent extends Event {
    private static final long serialVersionUID = 1L;
    private final boolean enableBots;
    private final boolean loadSavedGame;
    private final String saveFileName;

    public StartGameRequestEvent(boolean enableBots, boolean loadSavedGame, String saveFileName) {
        super("START_GAME_REQUEST");
        this.enableBots = enableBots;
        this.loadSavedGame = loadSavedGame;
        this.saveFileName = saveFileName;
    }

    public boolean isBotsEnabled() {
        return enableBots;
    }

    public boolean isLoadSavedGame() {
        return loadSavedGame;
    }

    public String getSaveFileName() {
        return saveFileName;
    }

    @Override
    public void process() {
        // Will be processed by server handler
    }
}
