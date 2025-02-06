package ludo.core.events.serverToClient;

import ludo.core.events.Event;

public class TurnChangeEvent extends Event {
    private static final long serialVersionUID = 1L;
    private final String currentPlayer;

    public TurnChangeEvent(String currentPlayer) {
        super("TURN_CHANGE");
        this.currentPlayer = currentPlayer;
    }

    public String getCurrentPlayer() {
        return currentPlayer;
    }

    @Override
    public void process() {
        // Will be processed by client handler
    }
}
