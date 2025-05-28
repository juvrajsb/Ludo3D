package ludo.core.events.serverToClient;

import ludo.core.events.Event;
import ludo.core.game.GameState;
import java.util.List;
import java.util.Map;

public class GameStateUpdateEvent extends Event {
    private static final long serialVersionUID = 1L;
    private final Map<String, List<Integer>> pawnPositions;
    private final String currentPlayer;
    private final GameState gameState;

    public GameStateUpdateEvent(Map<String, List<Integer>> pawnPositions,
                                String currentPlayer,
                                GameState gameState) {
        super("GAME_STATE_UPDATE");
        this.pawnPositions = pawnPositions;
        this.currentPlayer = currentPlayer;
        this.gameState = gameState;
    }

    public Map<String, List<Integer>> getPawnPositions() {
        return pawnPositions;
    }

    public String getCurrentPlayer() {
        return currentPlayer;
    }

    public GameState getGameState() {
        return gameState;
    }

    @Override
    public void process() {
        // Will be processed by client handler
    }
}
