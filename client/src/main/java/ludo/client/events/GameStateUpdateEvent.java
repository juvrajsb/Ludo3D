package ludo.client.events;

import java.util.List;
import java.util.Map;

public class GameStateUpdateEvent extends ClientEvent {
    private final Map<String, List<Integer>> pawnPositions;
    private final String currentPlayer;
    private final String gameState;

    public GameStateUpdateEvent(Map<String, List<Integer>> pawnPositions,
                                String currentPlayer,
                                String gameState) {
        this.eventType = "GAME_STATE_UPDATE";
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

    public String getGameState() {
        return gameState;
    }
}
