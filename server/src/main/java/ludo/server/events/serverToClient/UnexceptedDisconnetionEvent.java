package ludo.server.events.serverToClient;

import ludo.server.events.Event;
import ludo.server.handlers.UnexpectedDisconnectionHandler;

/**
 * Internal client event launched in case of
 * controller sudden disconnection.
 */
public class UnexceptedDisconnetionEvent extends Event {
    public UnexceptedDisconnetionEvent() {
        ID = "UNEXPECTED_DISCONNECTION";
    }

    @Override
    public void callHandler() {
        new UnexpectedDisconnectionHandler().handle(this);
    }
}
