package ludo.core.events.serverToClient;

import ludo.core.entities.Player;
import ludo.core.events.Event;

public class PlayerJoinedEvent extends Event {
    private static final long serialVersionUID = 1L;
    private final Player player;

    public PlayerJoinedEvent(Player player) {
        super("PLAYER_JOINED");
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public void process() {
        // Will be processed by client handler
    }
}
