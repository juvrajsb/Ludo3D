package ludo.core.events.serverToClient;

import ludo.core.events.Event;
import java.util.Map;
import java.util.List;

public class StateSyncErrorEvent extends Event {
    private static final long serialVersionUID = 1L;

    public enum ErrorType {
        POSITION_MISMATCH,
        PLAYER_MISMATCH,
        STATE_MISMATCH,
        SERVER_ERROR
    }

    private final ErrorType errorType;
    private final String errorMessage;
    private final Map<String, List<Integer>> expectedPositions;
    private final Map<String, List<Integer>> actualPositions;
    private final int retryCount;

    public StateSyncErrorEvent(ErrorType errorType, String errorMessage,
                              Map<String, List<Integer>> expectedPositions,
                              Map<String, List<Integer>> actualPositions,
                              int retryCount) {
        super("STATE_SYNC_ERROR");
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.expectedPositions = expectedPositions;
        this.actualPositions = actualPositions;
        this.retryCount = retryCount;
    }

    public StateSyncErrorEvent(ErrorType errorType, String errorMessage,
                              Map<String, List<Integer>> expectedPositions,
                              Map<String, List<Integer>> actualPositions) {
        this(errorType, errorMessage, expectedPositions, actualPositions, 0);
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Map<String, List<Integer>> getExpectedPositions() {
        return expectedPositions;
    }

    public Map<String, List<Integer>> getActualPositions() {
        return actualPositions;
    }

    public int getRetryCount() {
        return retryCount;
    }

    @Override
    public void process() {
        // This will be handled by the client's GameStateManager
    }
} 