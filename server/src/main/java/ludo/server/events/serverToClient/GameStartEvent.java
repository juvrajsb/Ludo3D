package ludo.server.events.serverToClient;

import ludo.core.entities.Player;
import ludo.server.events.Event;

import java.util.List;

// Event to notify about game start
public class GameStartEvent extends Event {
    private final List<Player> players;
    private final String startingPlayer;

    public GameStartEvent(List<Player> players, String startingPlayer) {
        ID = "GAME_START";
        this.players = players;
        this.startingPlayer = startingPlayer;
    }

    public List<Player> getPlayers() { return players; }
    public String getStartingPlayer() { return startingPlayer; }

    @Override
    public void callHandler() {
        // Client-side handler will initialize the game
    }
}
