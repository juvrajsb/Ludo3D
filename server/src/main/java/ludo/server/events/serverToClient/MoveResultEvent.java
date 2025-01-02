package ludo.server.events.serverToClient;

import ludo.server.events.Event;

// Event to notify about move results
public class MoveResultEvent extends Event {
    private final boolean success;
    private final String message;
    private final int pawnIndex;
    private final int newPosition;

    public MoveResultEvent(boolean success, String message, int pawnIndex, int newPosition) {
        ID = "MOVE_RESULT";
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
    public void callHandler() {
        // Client-side handler will update the pawn position
    }
}
