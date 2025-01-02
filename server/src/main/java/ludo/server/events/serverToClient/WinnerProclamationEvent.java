package ludo.server.events.serverToClient;
import java.util.List;

/**
 * Shows player ranking with points updated from
 * GoalsEvaluation as soon as received.
 * Immediately after closing connections with all players.
 * */
public class WinnerProclamationEvent {
    private final List<String> winners;

    public WinnerProclamationEvent(List<String> winners) {
        this.winners = winners;
    }

    public List<String> getWinners() {
        return winners;
    }
}
