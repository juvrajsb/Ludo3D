package ludo.core.events.clientToServer;

import ludo.core.events.Event;

public class SaveGameRequestEvent extends Event {
    private static final long serialVersionUID = 1L;
    private final boolean isAutoSave;

    public SaveGameRequestEvent(boolean isAutoSave) {
        super("SAVE_GAME_REQUEST");
        this.isAutoSave = isAutoSave;
    }

    public boolean isAutoSave() {
        return isAutoSave;
    }

    @Override
    public void process() {
        // Will be processed by server handler
    }
} 