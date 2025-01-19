package ludo.server.handlers;

import ludo.core.events.clientToServer.MoveRequestEvent;
import ludo.core.events.serverToClient.MoveResultEvent;
import ludo.core.events.serverToClient.GameStateUpdateEvent;
import ludo.core.game.GameManager;
import ludo.server.networking.EventTransmitter;
import ludo.server.Server;
import ludo.core.entities.Player;
import ludo.core.entities.Pawn;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MoveRequestHandler {
    private final GameManager gameManager;
    private final EventTransmitter eventTransmitter;

    public MoveRequestHandler() {
        this.gameManager = GameManager.getInstance();
        this.eventTransmitter = new EventTransmitter(Server.getInstance().getAllConnections());
    }

    public void handle(MoveRequestEvent event) {
        Player currentPlayer = gameManager.getCurrentPlayer();

        // Validate if it's the player's turn
        if (!isValidTurn(event, currentPlayer)) {
            sendMoveResult(event, false, "Not your turn", -1, -1);
            return;
        }

        // Validate pawn index
        if (!isValidPawnIndex(event.getPawnIndex())) {
            sendMoveResult(event, false, "Invalid pawn selected", event.getPawnIndex(), -1);
            return;
        }

        // Try to make the move
        boolean moveSuccess = gameManager.movePawn(
            currentPlayer,
            event.getPawnIndex(),
            event.getSteps()
        );

        if (moveSuccess) {
            // Get new position after successful move
            int newPosition = currentPlayer.getPawnPosition(event.getPawnIndex());

            // Send move result
            sendMoveResult(event, true, "Move successful", event.getPawnIndex(), newPosition);

            // Update game state for all players
            updateGameState();

            // Check for game end
            if (gameManager.hasPlayerWon(currentPlayer)) {
                handleGameEnd(currentPlayer);
            } else {
                gameManager.nextTurn();
            }
        } else {
            sendMoveResult(event, false, "Invalid move", event.getPawnIndex(), -1);
        }
    }

    private boolean isValidTurn(MoveRequestEvent event, Player currentPlayer) {
        return currentPlayer != null &&
            currentPlayer.getName().equals(event.getConnection().getConnectionID());
    }

    private boolean isValidPawnIndex(int pawnIndex) {
        return pawnIndex >= 0 && pawnIndex < 4; // Standard Ludo has 4 pawns per player
    }

    private void sendMoveResult(MoveRequestEvent event, boolean success,
                                String message, int pawnIndex, int newPosition) {
        MoveResultEvent resultEvent = new MoveResultEvent(
            success,
            message,
            pawnIndex,
            newPosition
        );

        try {
            eventTransmitter.broadcast(resultEvent);
        } catch (Exception e) {
            Server.LOGGER.severe("Failed to broadcast move result: " + e.getMessage());
        }
    }

    private void updateGameState() {
        // Collect current positions of all pawns for each player
        Map<String, List<Integer>> pawnPositions = new HashMap<>();

        for (Player player : gameManager.getPlayers()) {
            List<Integer> positions = player.getPawns().stream()
                .map(Pawn::getPosition)
                .collect(Collectors.toList());
            pawnPositions.put(player.getColor(), positions);
        }

        // Create and broadcast state update
        GameStateUpdateEvent stateEvent = new GameStateUpdateEvent(
            pawnPositions,
            gameManager.getCurrentPlayer().getColor(),
            gameManager.getGameState()
        );

        try {
            eventTransmitter.broadcast(stateEvent);
        } catch (Exception e) {
            Server.LOGGER.severe("Failed to broadcast game state: " + e.getMessage());
        }
    }

    private void handleGameEnd(Player winner) {
        // Implementation for game end handling
        // This would typically involve broadcasting a game over event
        // and updating the game state
    }
}
