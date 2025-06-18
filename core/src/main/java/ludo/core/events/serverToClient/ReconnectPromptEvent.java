package ludo.core.events.serverToClient;

import ludo.core.events.Event;

/**
 * Sent from the server to a client that has just connected,
 * if that client's username matches a player in an ongoing game.
 * This prompts the client to choose whether to rejoin the existing game
 * or start a new one.
 */
public class ReconnectPromptEvent extends Event {
    private static final long serialVersionUID = 1L;
    private final String playerName;

    public ReconnectPromptEvent(String playerName) {
        super("RECONNECT_PROMPT");
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }

    @Override
    public void process() {
        // Handled by the client
    }
}
