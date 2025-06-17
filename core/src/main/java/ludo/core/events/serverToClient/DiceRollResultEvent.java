package ludo.core.events.serverToClient;

import ludo.core.events.Event;

public class DiceRollResultEvent extends Event {
    private final int value;
    private final String playerColor;
    private static final long serialVersionUID = 1L;

    public DiceRollResultEvent(int value, String playerColor) {
        super("DICE_ROLL_RESULT");
        this.value = value;
        this.playerColor = playerColor;
    }

    public int getValue() {
        return value;
    }

    @Override
    public void process() {
        // Will be processed by client handler
    }
}
