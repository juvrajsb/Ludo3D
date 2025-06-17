//package ludo.core.events.serverToClient;
//
//import ludo.core.events.Event;
//
//public class SaveErrorEvent extends Event {
//    private static final long serialVersionUID = 1L;
//
//    public enum ErrorType {
//        NOT_AUTHORIZED,
//        INVALID_STATE,
//        INVALID_SAVE_DATA,
//        SAVE_FAILED,
//        SERVER_ERROR
//    }
//
//    private final ErrorType errorType;
//    private final String errorMessage;
//    private final boolean isAutoSave;
//    private final int retryCount;
//
//    public SaveErrorEvent(ErrorType errorType, String errorMessage, boolean isAutoSave, int retryCount) {
//        super("SAVE_ERROR");
//        this.errorType = errorType;
//        this.errorMessage = errorMessage;
//        this.isAutoSave = isAutoSave;
//        this.retryCount = retryCount;
//    }
//
//    public SaveErrorEvent(ErrorType errorType, String errorMessage, boolean isAutoSave) {
//        this(errorType, errorMessage, isAutoSave, 0);
//    }
//
//    public ErrorType getErrorType() {
//        return errorType;
//    }
//
//    public String getErrorMessage() {
//        return errorMessage;
//    }
//
//    public boolean isAutoSave() {
//        return isAutoSave;
//    }
//
//    public int getRetryCount() {
//        return retryCount;
//    }
//
//    @Override
//    public void process() {
//        // This will be handled by the client's GameStateManager
//    }
//}
