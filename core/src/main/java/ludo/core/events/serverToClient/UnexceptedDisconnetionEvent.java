package ludo.core.events.serverToClient;

import ludo.core.events.Event;

/**
 * Internal client event launched in case of
 * controller sudden disconnection.
 */
public class UnexceptedDisconnetionEvent extends Event {
    private static final long serialVersionUID = 1L;
    public UnexceptedDisconnetionEvent() {
        super("UNEXPECTED_DISCONNECTION");
    }

    @Override
    public void process() {
        // Will be processed by client handler
    }
}
