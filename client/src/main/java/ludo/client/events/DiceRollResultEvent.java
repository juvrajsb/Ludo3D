package ludo.client.events;

// Events client receives from controller
public class DiceRollResultEvent extends ClientEvent {
    private final int value;
    private final String playerColor;

    public DiceRollResultEvent(int value, String playerColor) {
        this.eventType = "DICE_ROLL_RESULT";
        this.value = value;
        this.playerColor = playerColor;
    }

    public int getValue() {
        return value;
    }

    public String getPlayerColor() {
        return playerColor;
    }
}
