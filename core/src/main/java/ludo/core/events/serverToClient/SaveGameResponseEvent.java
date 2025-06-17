package ludo.core.events.serverToClient;

import ludo.core.events.Event;

public class SaveGameResponseEvent extends Event {
    private static final long serialVersionUID = 1L;
    private final Response response;
    private final String saveFileName;
    private final String errorMessage;

    public enum Response {
        OK,
        NOT_AUTHORIZED,
        INVALID_STATE,
        SAVE_FAILED,
        SERVER_ERROR
    }

    public SaveGameResponseEvent(Response response, String saveFileName, String errorMessage) {
        super("SAVE_GAME_RESPONSE");
        this.response = response;
        this.saveFileName = saveFileName;
        this.errorMessage = errorMessage;
    }

    public SaveGameResponseEvent(Response response, String saveFileName) {
        this(response, saveFileName, null);
    }

    public SaveGameResponseEvent(Response response) {
        super("SAVE_GAME_RESPONSE");
        this.response = response;
        this.saveFileName = null;
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
