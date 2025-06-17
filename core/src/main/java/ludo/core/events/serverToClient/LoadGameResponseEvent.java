package ludo.core.events.serverToClient;

import ludo.core.events.Event;
import ludo.core.persistence.GamePersistence.GameSaveData;

public class LoadGameResponseEvent extends Event {
    private static final long serialVersionUID = 1L;
    private final Response response;
    private final GameSaveData saveData;
    private final String errorMessage;

    public enum Response {
        OK,
        NOT_AUTHORIZED,
        FILE_NOT_FOUND,
        INVALID_SAVE,
        INVALID_STATE,
        SERVER_ERROR
    }

    public LoadGameResponseEvent(Response response, GameSaveData saveData, String errorMessage) {
        super("LOAD_GAME_RESPONSE");
        this.response = response;
        this.saveData = saveData;
        this.errorMessage = errorMessage;
    }

    public LoadGameResponseEvent(Response response, GameSaveData saveData) {
        this(response, saveData, null);
    }

    public LoadGameResponseEvent(Response response) {
        super("LOAD_GAME_RESPONSE");
        this.response = response;
        this.saveData = null;
        this.errorMessage = null;
    }

    public Response getResponse() {
        return response;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public void process() {
        // Will be processed by client handler
    }
}
