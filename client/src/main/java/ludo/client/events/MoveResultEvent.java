package ludo.client.events;

public class MoveResultEvent extends ClientEvent {
    private final boolean success;
    private final String message;
    private final int pawnIndex;
    private final int newPosition;

    public MoveResultEvent(boolean success, String message, int pawnIndex, int newPosition) {
        this.eventType = "MOVE_RESULT";
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
}
