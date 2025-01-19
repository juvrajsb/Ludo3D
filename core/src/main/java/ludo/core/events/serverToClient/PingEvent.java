// PingEvent.java
package ludo.core.events.serverToClient;

import ludo.core.events.Event;

public class PingEvent extends Event {
    public PingEvent() {
        super("PING");
    }

    @Override
    public void process() {
        // Will be processed by client handler
    }
}
