package ludo.core.events.serverToClient;

import ludo.core.events.Event;
//import ludo.server.handlers.ForcedDisconnectionHandler;

/**
 * This event notices the client that the controller has
 * requested it to close the connection because
 * the excessive number of players in the waiting
 * queue after the decision made by the first player
 * about the number of players in the game.
 */
public class ForcedDisconnectionEvent extends Event {
    public ForcedDisconnectionEvent() {
        super("FORCED_DISCONNECTION");
    }

    @Override
    public void process() {
//        new ForcedDisconnectionHandler().handle(this);
    }
}
