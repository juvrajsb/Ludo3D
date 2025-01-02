package ludo.server.events.serverToClient;

import ludo.server.events.Event;
import ludo.server.handlers.ForcedDisconnectionHandler;

/**
 * This event notices the client that the controller has
 * requested it to close the connection because
 * the excessive number of players in the waiting
 * queue after the decision made by the first player
 * about the number of players in the game.
 */
public class ForcedDisconnectionEvent extends Event {
    public ForcedDisconnectionEvent() {
        ID = "FORCED_DISCONNECTION";
    }

    @Override
    public void callHandler() {
        new ForcedDisconnectionHandler().handle(this);
    }
}
