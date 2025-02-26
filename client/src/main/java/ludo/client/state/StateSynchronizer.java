package ludo.client.state;

import ludo.core.entities.Board;
import ludo.core.entities.Pawn;
import ludo.core.entities.Player;
import ludo.core.game.GameState;
import ludo.core.utils.Constants;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Handles state synchronization between client and server.
 * Ensures client state is reconciled with authoritative server state.
 */
public class StateSynchronizer {
    private static final Logger LOGGER = Logger.getLogger(StateSynchronizer.class.getName());

    /**
     * Reconciles client-side game state with server state
     */
    public static void reconcileGameState(List<Player> clientPlayers,
                                          Map<String, List<Integer>> serverPositions,
                                          GameState serverState) {
        LOGGER.info("Reconciling client state with server state");

        // Update pawn positions from server state
        for (Player player : clientPlayers) {
            String color = player.getColor();
            List<Integer> positions = serverPositions.get(color);

            if (positions != null) {
                for (int i = 0; i < positions.size() && i < player.getPawns().size(); i++) {
                    int serverPosition = positions.get(i);
                    int clientPosition = player.getPawns().get(i).getPosition();

                    if (serverPosition != clientPosition) {
                        LOGGER.info(String.format(
                            "Position mismatch for %s pawn %d - Client: %d, Server: %d",
                            color, i, clientPosition, serverPosition));

                        // Update client position to match server (authoritative)
                        player.getPawns().get(i).setPosition(serverPosition);

                        // Set home/finished state based on position
                        if (serverPosition == -1) {
                            player.getPawns().get(i).sendHome();
                        } else if (serverPosition >= Constants.BOARD_SIZE + Constants.HOME_COLUMN_SIZE - 1) {
                            player.getPawns().get(i).setFinished(true);
                        }
                    }
                }
            }
        }
    }

    /**
     * Predicts the result of a move before server confirmation
     */
    public static void predictMove(Player player, int pawnIndex, int steps, Board board) {
        // Only do prediction if valid parameters
        if (player == null || pawnIndex < 0 || pawnIndex >= player.getPawns().size() ||
            steps < 1 || steps > 6) {
            return;
        }

        Pawn pawn = player.getPawns().get(pawnIndex);

        // Store original position for reverting if needed
        int originalPosition = pawn.getPosition();
        boolean wasHome = pawn.isHome();
        boolean wasFinished = pawn.isFinished();

        // Predict move outcome
        if (pawn.isHome() && steps == 6) {
            // Leaving home prediction
            pawn.setPosition(board.getStartPosition(player.getColor()));
            pawn.leaveHome(board);
        } else if (!pawn.isHome() && !pawn.isFinished()) {
            // Regular move prediction
            int currentPosition = pawn.getPosition();
            int playerStartPos = board.getStartPosition(player.getColor());
            int entryPoint = (playerStartPos + board.getTotalSpaces() - 1) % board.getTotalSpaces();
            int newPosition = currentPosition + steps;

            // Handle home column entry
            if (currentPosition <= entryPoint && newPosition > entryPoint) {
                int stepsAfterEntry = newPosition - entryPoint - 1;
                if (stepsAfterEntry < board.getHomeColumnSize()) {
                    int homeColumnStart = board.getTotalSpaces() +
                        (playerStartPos / 13) * board.getHomeColumnSize();
                    pawn.setPosition(homeColumnStart + stepsAfterEntry);

                    // Check if pawn has reached final home position
                    if (stepsAfterEntry == board.getHomeColumnSize() - 1) {
                        pawn.setFinished(true);
                    }
                }
            } else if (currentPosition >= board.getTotalSpaces()) {
                // Movement within home column
                int homeColumnStart = board.getTotalSpaces() +
                    (playerStartPos / 13) * board.getHomeColumnSize();
                int homeColumnEnd = homeColumnStart + board.getHomeColumnSize() - 1;

                if (newPosition <= homeColumnEnd) {
                    pawn.setPosition(newPosition);
                    // Check if pawn has reached final home position
                    if (newPosition == homeColumnEnd) {
                        pawn.setFinished(true);
                    }
                }
            } else {
                // Regular board movement
                pawn.setPosition(newPosition % board.getTotalSpaces());
            }
        }

        LOGGER.info(String.format(
            "Move prediction for %s pawn %d: %d -> %d",
            player.getColor(), pawnIndex, originalPosition, pawn.getPosition()
        ));
    }

    /**
     * Reverts a predicted move if server rejects it
     */
    public static void revertPrediction(Player player, int pawnIndex, int originalPosition,
                                        boolean wasHome, boolean wasFinished) {
        if (player == null || pawnIndex < 0 || pawnIndex >= player.getPawns().size()) {
            return;
        }

        Pawn pawn = player.getPawns().get(pawnIndex);
        pawn.setPosition(originalPosition);

        if (wasHome) {
            pawn.sendHome();
        }

        pawn.setFinished(wasFinished);

        LOGGER.info(String.format(
            "Reverted prediction for %s pawn %d back to position %d",
            player.getColor(), pawnIndex, originalPosition
        ));
    }
}
