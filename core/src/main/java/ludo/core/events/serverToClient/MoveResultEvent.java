package ludo.core.events.serverToClient;

import ludo.core.events.Event;

public class MoveResultEvent extends Event {
    private final boolean success;
    private final String message;
    private final int pawnIndex;
    private final int newPosition;

    public MoveResultEvent(boolean success, String message, int pawnIndex, int newPosition) {
        super("MOVE_RESULT");
        this.success = success;
        this.message = message;
        this.pawnIndex = pawnIndex;
        this.newPosition = newPosition;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public int getPawnIndex() {
        return pawnIndex;
    }

    public int getNewPosition() {
        return newPosition;
    }

    @Override
    public void process() {
        // Will be processed by client handler
    }
}
