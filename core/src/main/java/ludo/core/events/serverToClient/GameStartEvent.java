package ludo.core.events.serverToClient;

import ludo.core.entities.Player;
import ludo.core.events.Event;
import java.util.List;

public class GameStartEvent extends Event {
    private final List<Player> players;
    private final String startingPlayer;

    public GameStartEvent(List<Player> players, String startingPlayer) {
        super("GAME_START");
        this.players = players;
        this.startingPlayer = startingPlayer;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public String getStartingPlayer() {
        return startingPlayer;
    }

    @Override
    public void process() {
        // Will be processed by client handler
    }
}
