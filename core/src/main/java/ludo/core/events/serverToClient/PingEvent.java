// PingEvent.java
package ludo.core.events.serverToClient;

import ludo.core.events.Event;

public class PingEvent extends Event {
    private static final long serialVersionUID = 1L;
    public PingEvent() {
        super("PING");
    }

    @Override
    public void process() {
        // Will be processed by client handler
    }
}
