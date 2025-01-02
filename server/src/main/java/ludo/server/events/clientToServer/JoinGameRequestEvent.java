package ludo.server.events.clientToServer;

import ludo.server.events.Event;
import ludo.server.handler.ClientJoinHandler;

// Event for joining a game
public class JoinGameRequestEvent extends Event {
    private final String playerName;
    private final String desiredColor;

    public JoinGameRequestEvent(String playerName, String desiredColor) {
        ID = "JOIN_GAME_REQUEST";
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
    public void callHandler() {
        new ClientJoinHandler().handle(this);
    }
}
