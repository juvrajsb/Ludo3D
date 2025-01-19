package ludo.core.events.serverToClient;

import ludo.core.events.Event;
//import ludo.server.handlers.UnexpectedDisconnectionHandler;

/**
 * Internal client event launched in case of
 * controller sudden disconnection.
 */
public class UnexceptedDisconnetionEvent extends Event {
    public UnexceptedDisconnetionEvent() {
        super("UNEXPECTED_DISCONNECTION");
    }

    @Override
    public void process() {
//        new UnexpectedDisconnectionHandler().handle(this);
    }
}
