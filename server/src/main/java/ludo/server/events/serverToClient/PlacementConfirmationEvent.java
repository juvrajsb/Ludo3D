package ludo.server.events.serverToClient;

import ludo.server.events.Event;

public class PlacementConfirmationEvent extends Event {
    private final int gainedPoints;

    public PlacementConfirmationEvent(int gainedPoints) {
        ID = "PLACEMENT_CONFIRMATION";
        this.gainedPoints = gainedPoints;
    }

    public int getGainedPoints() {
        return gainedPoints;
    }

    @Override
    public void callHandler() {

    }
}
