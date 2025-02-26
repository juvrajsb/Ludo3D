package ludo.client.state;

import ludo.core.entities.Board;
import ludo.core.entities.Pawn;
import ludo.core.entities.Player;
import ludo.core.game.GameState;

import java.util.*;
import java.util.logging.Logger;

/**
 * Manages client-side prediction for improved responsiveness
 */
public class ClientPredictionManager {
    private static final Logger LOGGER = Logger.getLogger(ClientPredictionManager.class.getName());

    // Track pending predictions (moves waiting for server confirmation)
    private static class PendingPrediction {
        final String playerColor;
        final int pawnIndex;
        final int fromPosition;
        final boolean wasHome;
        final boolean wasFinished;
        final long timestamp;

        PendingPrediction(String playerColor, int pawnIndex, int fromPosition,
                          boolean wasHome, boolean wasFinished) {
            this.playerColor = playerColor;
            this.pawnIndex = pawnIndex;
            this.fromPosition = fromPosition;
            this.wasHome = wasHome;
            this.wasFinished = wasFinished;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private final Queue<PendingPrediction> pendingPredictions = new LinkedList<>();
    private final Map<Integer, GameState> predictedGameStates = new HashMap<>();
    private final Map<Integer, Map<String, List<Integer>>> predictedPositions = new HashMap<>();
    private int predictionCounter = 0;

    /**
     * Records a client-side prediction
     */
    public int recordPrediction(Player player, int pawnIndex, int steps, Board board) {
        if (player == null || pawnIndex < 0 || pawnIndex >= player.getPawns().size()) {
            return -1;
        }

        Pawn pawn = player.getPawns().get(pawnIndex);
        int fromPosition = pawn.getPosition();
        boolean wasHome = pawn.isHome();
        boolean wasFinished = pawn.isFinished();

        // Record prediction
        PendingPrediction prediction = new PendingPrediction(
            player.getColor(), pawnIndex, fromPosition, wasHome, wasFinished);

        pendingPredictions.add(prediction);

        // Generate prediction ID
        int predictionId = ++predictionCounter;

        // Store predicted state
        GameState currentState = GameState.PLAYER_MOVED; // Predicted next state
        predictedGameStates.put(predictionId, currentState);

        // Store predicted positions
        Map<String, List<Integer>> positions = getCurrentPositions();
        predictedPositions.put(predictionId, positions);

        LOGGER.info(String.format(
            "Recorded prediction #%d: %s pawn %d, steps %d, from %d",
            predictionId, player.getColor(), pawnIndex, steps, fromPosition
        ));

        return predictionId;
    }

    /**
     * Confirms a prediction with server result
     */
    public void confirmPrediction(int predictionId, boolean success,
                                  int actualPosition, GameState serverState) {
        // Clean up prediction data
        predictedGameStates.remove(predictionId);
        predictedPositions.remove(predictionId);

        if (!success) {
            // Server rejected - need to revert the oldest pending prediction
            if (!pendingPredictions.isEmpty()) {
                PendingPrediction oldest = pendingPredictions.poll();

                LOGGER.warning(String.format(
                    "Server rejected prediction for %s pawn %d - reverting from %d",
                    oldest.playerColor, oldest.pawnIndex, oldest.fromPosition
                ));

                // Revert would happen in GameStateManager
            }
        } else {
            // Server confirmed - remove the prediction
            if (!pendingPredictions.isEmpty()) {
                pendingPredictions.poll();

                LOGGER.info(String.format(
                    "Server confirmed prediction #%d - actual position: %d",
                    predictionId, actualPosition
                ));
            }
        }
    }

    /**
     * Check if there are pending predictions
     */
    public boolean hasPendingPredictions() {
        return !pendingPredictions.isEmpty();
    }

    /**
     * Get the oldest pending prediction (for reconciliation)
     */
    public PendingPrediction getOldestPrediction() {
        return pendingPredictions.peek();
    }

    /**
     * Get current positions map (for prediction)
     */
    private Map<String, List<Integer>> getCurrentPositions() {
        // This would get the current positions from the game state
        // Implementation depends on your game structure
        return new HashMap<>(); // Placeholder
    }

    /**
     * Clean up old predictions (older than timeout)
     */
    public void cleanupOldPredictions(long timeoutMs) {
        long now = System.currentTimeMillis();

        while (!pendingPredictions.isEmpty()) {
            PendingPrediction oldest = pendingPredictions.peek();

            if (now - oldest.timestamp > timeoutMs) {
                // Remove expired prediction
                pendingPredictions.poll();
                LOGGER.warning("Removed expired prediction - timed out waiting for server");
            } else {
                // No more expired predictions
                break;
            }
        }
    }
}
