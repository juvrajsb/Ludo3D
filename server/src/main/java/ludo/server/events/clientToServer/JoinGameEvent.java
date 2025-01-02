package ludo.server.events.clientToServer;

import ludo.server.events.Event;

/**
 * Event that represent a Client joining a game.
 * It contains the username that the Client wants
 * to use in the game.
 */
public class JoinGameEvent extends Event {
    private final String username;

    public JoinGameEvent(String username) {
        this.ID = "JOIN_GAME_EVENT";
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void callHandler() {
        ConnectionsController.getInstance().handle(this);
    }
}
