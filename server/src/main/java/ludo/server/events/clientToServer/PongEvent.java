package ludo.server.events.clientToServer;

import ludo.server.handler.PongHandler;
import ludo.server.events.Event;

public class PongEvent extends Event {
    public PongEvent() {
        ID = "PONG_EVENT";
    }

    @Override
    public void callHandler() {
        new PongHandler().handle(this);
    }
}
