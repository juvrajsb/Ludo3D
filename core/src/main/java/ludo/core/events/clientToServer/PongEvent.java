// PongEvent.java
package ludo.core.events.clientToServer;

import ludo.core.events.Event;

public class PongEvent extends Event {
    public PongEvent() {
        super("PONG");
    }

    @Override
    public void process() {
        // Will be processed by server handler
    }
}
