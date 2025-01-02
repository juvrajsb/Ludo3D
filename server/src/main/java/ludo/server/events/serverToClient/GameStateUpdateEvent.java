package ludo.server.events.serverToClient;

import ludo.server.events.Event;

import java.util.Map;
import java.util.List;

// Event to notify clients about game state updates
public class GameStateUpdateEvent extends Event {
    private final Map<String, List<Integer>> pawnPositions;
    private final String currentPlayer;
    private final GameState gameState;

    public GameStateUpdateEvent(Map<String, List<Integer>> pawnPositions,
                                String currentPlayer,
                                GameState gameState) {
        ID = "GAME_STATE_UPDATE";
        this.pawnPositions = pawnPositions;
        this.currentPlayer = currentPlayer;
        this.gameState = gameState;
    }

    public Map<String, List<Integer>> getPawnPositions() { return pawnPositions; }
    public String getCurrentPlayer() { return currentPlayer; }
    public GameState getGameState() { return gameState; }

    @Override
    public void callHandler() {
        // Client-side handler will update the game state
    }
}

