package ludo.core.events.serverToClient;

import ludo.core.events.Event;

public class LoadErrorEvent extends Event {
    private static final long serialVersionUID = 1L;

    public enum ErrorType {
        FILE_NOT_FOUND,
        INVALID_SAVE_DATA,
        STATE_CONFLICT,
        SERVER_ERROR
    }

    private final ErrorType errorType;
    private final String errorMessage;
    private final String saveFileName;
    private final int retryCount;

    public LoadErrorEvent(ErrorType errorType, String errorMessage, String saveFileName, int retryCount) {
        super("LOAD_ERROR");
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.saveFileName = saveFileName;
        this.retryCount = retryCount;
    }

    public LoadErrorEvent(ErrorType errorType, String errorMessage, String saveFileName) {
        this(errorType, errorMessage, saveFileName, 0);
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getSaveFileName() {
        return saveFileName;
    }

    public int getRetryCount() {
        return retryCount;
    }

    @Override
    public void process() {
        // This will be handled by the client's GameStateManager
    }
} 