package ludo.server.events.serverToClient;

import ludo.server.events.Event;

// Event to notify about dice roll results
public class DiceRollResultEvent extends Event {
    private final int value;
    private final String playerColor;

    public DiceRollResultEvent(int value, String playerColor) {
        ID = "DICE_ROLL_RESULT";
        this.value = value;
        this.playerColor = playerColor;
    }

    public int getValue() {
        return value;
    }

    public String getPlayerColor() {
        return playerColor;
    }

    @Override
    public void callHandler() {
        // Client-side handler will update the dice display
    }
}
