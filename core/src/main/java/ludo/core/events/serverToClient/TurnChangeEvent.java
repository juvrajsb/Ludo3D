// PlayerJoinedEvent.java
package ludo.core.events.serverToClient;

import ludo.core.entities.Player;
import ludo.core.events.Event;

public class TurnChangeEvent extends Event {

    public TurnChangeEvent(Player player) {
        super("TURN_CHANGE");
    }

    @Override
    public void process() {
        // Will be processed by client handler
    }

    public String getCurrentPlayer() {
        return null;
    }
}
