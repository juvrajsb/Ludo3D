package ludo.server.handlers;

import ludo.core.handlers.AbstractServerEventHandler;
import ludo.core.events.clientToServer.*;
import ludo.core.events.serverToClient.*;
import ludo.core.network.Connection;
import ludo.core.game.GameManager;
import ludo.server.session.SessionManager;

import java.io.IOException;

public class ServerGameHandler extends AbstractServerEventHandler {
    private final GameManager gameManager;
    private final SessionManager sessionManager;

    public ServerGameHandler(GameManager gameManager, SessionManager sessionManager) {
        this.gameManager = gameManager;
        this.sessionManager = sessionManager;
    }

    @Override
    public void handleDiceRollRequest(DiceRollRequestEvent event) {
        // Validate turn
        if (!gameManager.isPlayerTurn(event.getConnection().getConnectionID())) {
            return;
        }

        // Process dice roll
        int value = gameManager.rollDice();
        String playerColor = gameManager.getCurrentPlayer().getColor();

        // Broadcast result
        DiceRollResultEvent resultEvent = new DiceRollResultEvent(value, playerColor);
        sessionManager.broadcastEvent(resultEvent);
    }

    @Override
    public void handleMoveRequest(MoveRequestEvent event) {
        String playerId = event.getConnection().getConnectionID();

        // Validate turn
        if (!gameManager.isPlayerTurn(playerId)) {
            sendMoveResult(event, false, "Not your turn", -1);
            return;
        }

        // Process move
        boolean success = gameManager.movePawn(
            playerId,
            event.getPawnIndex(),
            event.getSteps()
        );

        if (success) {
            int newPosition = gameManager.getPawnPosition(playerId, event.getPawnIndex());
            sendMoveResult(event, true, "Move successful", newPosition);

            // Check for game over
            if (gameManager.hasPlayerWon(playerId)) {
                handleGameOver(playerId);
            } else {
                gameManager.nextTurn();
                broadcastGameState();
            }
        } else {
            sendMoveResult(event, false, "Invalid move", -1);
        }
    }

    @Override
    public void handleJoinRequest(JoinGameRequestEvent event) {
        // Handle player joining via session manager
        boolean joined = sessionManager.handlePlayerJoin(
            event.getPlayerName(),
            event.getDesiredColor(),
            event.getConnection()
        );

        if (joined) {
            broadcastGameState();
        }
    }

    @Override
    public void handleLeaveRequest(LeaveGameRequestEvent event) {
        sessionManager.handlePlayerLeave(event.getConnection().getConnectionID());
        broadcastGameState();
    }

    @Override
    public void handleStartGameRequest(StartGameRequestEvent event) {
        if (gameManager.canStartGame()) {
            gameManager.startGame();
            broadcastGameState();
        }
    }

    @Override
    public void handlePlayerDisconnected(Connection connection) {
        sessionManager.handleDisconnection(connection.getConnectionID());
    }

    @Override
    public void handlePlayerReconnected(Connection connection) {
        sessionManager.handleReconnection(connection);
        // Send current game state to reconnected player
        try {
            connection.send(gameManager.createGameStateEvent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handlePong(PongEvent event) {
        sessionManager.updatePlayerActivity(event.getConnection().getConnectionID());
    }

    private void sendMoveResult(MoveRequestEvent event, boolean success,
                                String message, int newPosition) {
        MoveResultEvent resultEvent = new MoveResultEvent(
            success, message, event.getPawnIndex(), newPosition
        );
        sessionManager.broadcastEvent(resultEvent);
    }

    private void broadcastGameState() {
        GameStateUpdateEvent stateEvent = gameManager.createGameStateEvent();
        sessionManager.broadcastEvent(stateEvent);
    }

    private void handleGameOver(String winnerId) {
        GameOverEvent gameOverEvent = new GameOverEvent(winnerId);
        sessionManager.broadcastEvent(gameOverEvent);
    }
}
