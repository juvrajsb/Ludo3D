package ludo.core.events.clientToServer;

import ludo.core.events.Event;

public class ReconnectRequestEvent extends Event {
    private static final long serialVersionUID = 1L;
    private final String playerName;

    public ReconnectRequestEvent(String playerName) {
        super("RECONNECT_REQUEST");
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }

    @Override
    public void process() {
        // Will be processed by server handler
    }
}
