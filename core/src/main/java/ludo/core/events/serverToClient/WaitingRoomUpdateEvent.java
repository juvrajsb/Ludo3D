package ludo.core.events.serverToClient;

import ludo.core.events.Event;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Event containing the updated list of the
 * usernames of the players inside controller's
 * waiting room.
 */
public class WaitingRoomUpdateEvent extends Event {
    private static final long serialVersionUID = 1L;
    private final List<String> usernames;
    private final Map<String, String> playerColors;
    private final int numberOfPlayers;

    public WaitingRoomUpdateEvent(List<String> usernames, Map<String, String> colors, int numberOfPlayers) {
        super("WAITING_ROOM_UPDATE");
        this.usernames = usernames;
        this.playerColors = colors;
        this.numberOfPlayers = numberOfPlayers;
    }

    public List<String> getUsernames() {
        return usernames;
    }

    public String getColorForPlayer(String username) {
        return playerColors.getOrDefault(username, "");
    }

    @Override
    public void process() {
        // Will be processed by client handler
    }
}
