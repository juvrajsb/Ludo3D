package ludo.core.events.serverToClient;
import ludo.core.events.Event;

import java.util.List;

/**
 * Shows player ranking with points updated from
 * GoalsEvaluation as soon as received.
 * Immediately after closing connections with all players.
 * */
public class WinnerProclamationEvent extends Event {
    private static final long serialVersionUID = 1L;
    private final List<String> winners;

    public WinnerProclamationEvent(List<String> winners) {
        super("WINNER_PROCLAMATION");
        this.winners = winners;
    }

    public List<String> getWinners() {
        return winners;
    }

    @Override
    public void process() {
        // Will be processed by client handler
    }
}
