package ludo.core.events.clientToServer;

import ludo.core.events.Event;

public class LoadGameRequestEvent extends Event {
    private static final long serialVersionUID = 1L;
    private final String saveFileName;
    private final boolean isAutoSave;

    public LoadGameRequestEvent(String saveFileName, boolean isAutoSave) {
        super("LOAD_GAME_REQUEST");
        this.saveFileName = saveFileName;
        this.isAutoSave = isAutoSave;
    }

    public String getSaveFileName() {
        return saveFileName;
    }

    public boolean isAutoSave() {
        return isAutoSave;
    }

    @Override
    public void process() {
        // Will be processed by server handler
    }
} 