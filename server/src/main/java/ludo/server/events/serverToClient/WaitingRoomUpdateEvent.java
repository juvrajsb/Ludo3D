package ludo.server.events.serverToClient;

import ludo.server.events.Event;
import ludo.server.handlers.WaitingRoomUpdateHandler;
import java.util.List;

/**
 * Event containing the updated list of the
 * usernames of the players inside controller's
 * waiting room.
 */
public class WaitingRoomUpdateEvent extends Event {
    private final List<String> usernames;
    private final int numberOfPlayers;

    public WaitingRoomUpdateEvent(List<String> usernames, int numberOfPlayers) {
        ID = "WAITING_ROOM_UPDATE";
        this.usernames = usernames;
        this.numberOfPlayers = numberOfPlayers;
    }

    public List<String> getUsernames() {
        return usernames;
    }

    public int getNumberOfPlayers() {
        return numberOfPlayers;
    }

    @Override
    public void callHandler() {
        new WaitingRoomUpdateHandler().handle(this);
    }
}
