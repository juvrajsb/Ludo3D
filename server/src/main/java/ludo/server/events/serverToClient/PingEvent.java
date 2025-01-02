package ludo.server.events.serverToClient;

import ludo.server.events.Event;
import ludo.server.handlers.PingHandler;

public class PingEvent extends Event {
    public PingEvent() {
        ID = "PING_EVENT";
    }

    @Override
    public void callHandler() {
        new PingHandler().handle(this);
    }
}
