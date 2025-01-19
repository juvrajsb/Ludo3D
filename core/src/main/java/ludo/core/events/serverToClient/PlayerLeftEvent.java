// PlayerLeftEvent.java
package ludo.core.events.serverToClient;

import ludo.core.events.Event;

public class PlayerLeftEvent extends Event {
    private final String playerName;

    public PlayerLeftEvent(String playerName) {
        super("PLAYER_LEFT");
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }

    @Override
    public void process() {
        // Will be processed by client handler
    }
}
