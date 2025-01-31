package ludo.core.events.clientToServer;

import ludo.core.events.Event;

public class MoveRequestEvent extends Event {
    private static final long serialVersionUID = 1L;

    private final int pawnIndex;
    private final int steps;

    public MoveRequestEvent(int pawnIndex, int steps) {
        super("MOVE_REQUEST");
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
    public void process() {
        // Will be processed by server handler
    }
}
