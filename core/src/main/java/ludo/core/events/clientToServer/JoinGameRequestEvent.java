package ludo.core.events.clientToServer;

import ludo.core.events.Event;

public class JoinGameRequestEvent extends Event {
    private static final long serialVersionUID = 1L;

    private final String playerName;
    private final String desiredColor;

    public JoinGameRequestEvent(String playerName, String desiredColor) {
        super("JOIN_GAME_REQUEST");
        this.playerName = playerName;
        this.desiredColor = desiredColor;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getDesiredColor() {
        return desiredColor;
    }

    @Override
    public void process() {
        // Will be processed by server handler
    }
}
