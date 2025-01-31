package ludo.core.events.serverToClient;

import ludo.core.entities.Player;
import ludo.core.events.Event;
import java.util.List;

public class GameStartedEvent extends Event {
    private static final long serialVersionUID = 1L;
    private final List<Player> players;
    private final String startingPlayer;
    private final int totalPlayers;

    public GameStartedEvent(List<Player> players, String startingPlayer, int totalPlayers) {
        super("GAME_STARTED");
        this.players = players;
        this.startingPlayer = startingPlayer;
        this.totalPlayers = totalPlayers;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public String getStartingPlayer() {
        return startingPlayer;
    }

    public int getTotalPlayers() {
        return totalPlayers;
    }

    @Override
    public void process() {
        // Will be processed by client handler
    }
}
