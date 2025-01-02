package ludo.client.events;

import ludo.core.entities.Player;

import java.util.List;

public class GameStartEvent extends ClientEvent {
    private final List<Player> players;
    private final String startingPlayer;

    public GameStartEvent(List<Player> players, String startingPlayer) {
        this.eventType = "GAME_START";
        this.players = players;
        this.startingPlayer = startingPlayer;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public String getStartingPlayer() {
        return startingPlayer;
    }
}
