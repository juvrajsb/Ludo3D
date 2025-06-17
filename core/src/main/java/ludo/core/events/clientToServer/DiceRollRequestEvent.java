package ludo.core.events.clientToServer;

import ludo.core.events.Event;

public class DiceRollRequestEvent extends Event {
    private static final long serialVersionUID = 1L;

    public DiceRollRequestEvent() {
        super("DICE_ROLL_REQUEST");
    }

    @Override
    public void process() {
        // Will be processed by server handler
    }
}
