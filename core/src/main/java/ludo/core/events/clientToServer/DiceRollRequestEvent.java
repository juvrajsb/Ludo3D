package ludo.core.events.clientToServer;

import ludo.core.events.Event;

public class DiceRollRequestEvent extends Event {
    public DiceRollRequestEvent() {
        super("DICE_ROLL_REQUEST");
    }

    @Override
    public void process() {
        // Will be processed by server handler
    }

    public int getValue() {
        return 0;
    }
}
