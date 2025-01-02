package ludo.server.events.clientToServer;

import ludo.server.events.Event;
import ludo.server.handler.GamePlayHandler;

// Event for moving a pawn
public class MoveRequestEvent extends Event {
    private final int pawnIndex;
    private final int steps;

    public MoveRequestEvent(int pawnIndex, int steps) {
        ID = "MOVE_REQUEST";
        this.pawnIndex = pawnIndex;
        this.steps = steps;
    }

    public int getPawnIndex() {
        return pawnIndex;
    }

    public int getSteps() {
        return steps;
    }

    @Override
    public void callHandler() {
        new GamePlayHandler().handleMove(this);
    }
}
