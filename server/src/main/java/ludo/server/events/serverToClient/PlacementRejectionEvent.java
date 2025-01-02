package ludo.server.events.serverToClient;

import ludo.server.events.Event;

public class PlacementRejectionEvent extends Event {
    public PlacementRejectionEvent() {
        ID = "PLACEMENT_REJECTION";
    }
    @Override
    public void callHandler() {

    }
}
