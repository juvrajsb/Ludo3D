package ludo.client.events;

// Events client sends to controller
public class JoinGameRequest extends ClientEvent {
    private final String playerName;
    private final String desiredColor;

    public JoinGameRequest(String playerName, String desiredColor) {
        this.eventType = "JOIN_GAME_REQUEST";
        this.playerName = playerName;
        this.desiredColor = desiredColor;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getDesiredColor() {
        return desiredColor;
    }
}
