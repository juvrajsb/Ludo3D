// GameOverEvent.java
package ludo.core.events.serverToClient;

import ludo.core.events.Event;

public class GameOverEvent extends Event {
    private final String winner;

    public GameOverEvent(String winner) {
        super("GAME_OVER");
        this.winner = winner;
    }

    public String getWinner() {
        return winner;
    }

    @Override
    public void process() {
        // Will be processed by client handler
    }
}
