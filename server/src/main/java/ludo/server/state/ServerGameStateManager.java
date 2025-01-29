package ludo.server.state;

import ludo.core.entities.*;
import ludo.core.game.GameState;
import ludo.core.game.GameManager;
import ludo.core.network.Connection;
import ludo.server.networking.ServerNetworkHandler;
import ludo.server.session.SessionManager;
import ludo.core.events.serverToClient.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class ServerGameStateManager {
    private static final Logger LOGGER = Logger.getLogger(ServerGameStateManager.class.getName());

    private final GameManager gameManager;
    private final SessionManager sessionManager;
    private final ServerNetworkHandler networkHandler;
    private final Map<String, String> connectionToPlayerMap; // Maps connectionIds to playerNames

    public ServerGameStateManager(ServerNetworkHandler networkHandler) {
        this.gameManager = GameManager.getInstance();
        this.sessionManager = SessionManager.getInstance();
        this.networkHandler = networkHandler;
        this.connectionToPlayerMap = new ConcurrentHashMap<>();
    }

    // Player Management
    public boolean handlePlayerJoin(String connectionId, String playerName, String desiredColor) {
        if (gameManager.getPlayers().size() >= 4 || gameManager.isGameStarted()) {
            return false;
        }

        // Check if color is available
        if (!isColorAvailable(desiredColor)) {
            return false;
        }

        Player newPlayer = new Player(playerName, desiredColor);
        if (gameManager.addPlayer(newPlayer)) {
            connectionToPlayerMap.put(connectionId, playerName);

            // Notify other players
            PlayerJoinedEvent joinEvent = new PlayerJoinedEvent(newPlayer);
            networkHandler.broadcast(joinEvent);

            // Send updated game state
            broadcastGameState();
            return true;
        }
        return false;
    }

    public void handlePlayerLeave(String connectionId) {
        String playerName = connectionToPlayerMap.get(connectionId);
        if (playerName != null) {
            connectionToPlayerMap.remove(connectionId);

            // Notify other players
            PlayerLeftEvent leaveEvent = new PlayerLeftEvent(playerName);
            networkHandler.broadcast(leaveEvent);

            if (!gameManager.isGameStarted()) {
                // Only remove player if game hasn't started
                gameManager.removePlayer(playerName);
            }
            broadcastGameState();
        }
    }

    // Game Actions
    public void handleDiceRoll(String connectionId) {
        String playerName = connectionToPlayerMap.get(connectionId);
        if (playerName != null && gameManager.isPlayerTurn(playerName)) {
            int value = gameManager.rollDice();
            DiceRollResultEvent event = new DiceRollResultEvent(
                value,
                gameManager.getCurrentPlayer().getColor()
            );
            networkHandler.broadcast(event);
        }
    }

    public boolean handleMove(String connectionId, int pawnIndex, int steps) {
        String playerName = connectionToPlayerMap.get(connectionId);
        if (!gameManager.isPlayerTurn(playerName)) {
            return false;
        }

        boolean moveSuccess = gameManager.movePawn(playerName, pawnIndex, steps);

        // Create and broadcast move result
        MoveResultEvent resultEvent = new MoveResultEvent(
            moveSuccess,
            moveSuccess ? "Move successful" : "Invalid move",
            pawnIndex,
            moveSuccess ? gameManager.getPawnPosition(playerName, pawnIndex) : -1
        );
        networkHandler.broadcast(resultEvent);

        if (moveSuccess) {
            // Check win condition
            if (gameManager.hasPlayerWon(playerName)) {
                handleGameOver(playerName);
            } else {
                gameManager.nextTurn();
                broadcastGameState();
            }
        }

        return moveSuccess;
    }

    // State Management
    private void broadcastGameState() {
        GameStateUpdateEvent stateEvent = new GameStateUpdateEvent(
            gameManager.getCurrentPawnPositions(),
            gameManager.getCurrentPlayer().getColor(),
            gameManager.getGameState()
        );
        networkHandler.broadcast(stateEvent);
    }

    private void handleGameOver(String winner) {
        GameOverEvent gameOverEvent = new GameOverEvent(winner);
        networkHandler.broadcast(gameOverEvent);
        // Update game state
        gameManager.setGameState(GameState.GAME_OVER);
        broadcastGameState();
    }

    // Connection Management
    public void handleDisconnection(String connectionId) {
        handlePlayerLeave(connectionId);
        sessionManager.handleDisconnection(connectionId);
    }

    public void handleReconnection(String connectionId, Connection connection) {
        String playerName = connectionToPlayerMap.get(connectionId);
        if (playerName != null) {
            sessionManager.handleReconnection(connection);
            // Send current game state to reconnected player
            GameStateUpdateEvent stateEvent = new GameStateUpdateEvent(
                gameManager.getCurrentPawnPositions(),
                gameManager.getCurrentPlayer().getColor(),
                gameManager.getGameState()
            );
            networkHandler.sendToClient(connectionId, stateEvent);
        }
    }

    // Utility Methods
    private boolean isColorAvailable(String color) {
        return gameManager.getPlayers().stream()
            .noneMatch(p -> p.getColor().equals(color));
    }

    public boolean isPlayerTurn(String connectionId) {
        String playerName = connectionToPlayerMap.get(connectionId);
        return playerName != null && gameManager.isPlayerTurn(playerName);
    }

    public GameState getGameState() {
        return gameManager.getGameState();
    }

    public boolean isGameStarted() {
        return gameManager.isGameStarted();
    }

    public String getPlayerName(String connectionId) {
        return connectionToPlayerMap.get(connectionId);
    }
}

//package ludo.server.state;
//
//import ludo.core.entities.Player;
//import ludo.core.game.GameManager;
//import ludo.core.game.GameState;
//import ludo.server.networking.ServerNetworkHandler;
//import ludo.server.session.SessionManager;
//import ludo.core.events.serverToClient.*;
//
//    import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.logging.Logger;
//
//public class ServerGameStateManager {
//    private static final Logger LOGGER = Logger.getLogger(ServerGameStateManager.class.getName());
//
//    private final GameManager gameManager;
//    private final SessionManager sessionManager;
//    private final ServerNetworkHandler networkHandler;
//    private final Map<String, String> connectionToPlayerMap;
//
//    public ServerGameStateManager(ServerNetworkHandler networkHandler) {
//        this.gameManager = GameManager.getInstance();
//        this.sessionManager = SessionManager.getInstance();
//        this.networkHandler = networkHandler;
//        this.connectionToPlayerMap = new ConcurrentHashMap<>();
//    }
//
//    // Only handle the mapping between connections and game state
//    public boolean handlePlayerJoin(String connectionId, String playerName, String color) {
//        Player newPlayer = new Player(playerName, color);
//        if (gameManager.addPlayer(newPlayer)) {
//            connectionToPlayerMap.put(connectionId, playerName);
//            broadcastGameState();
//            return true;
//        }
//        return false;
//    }
//
//    public void handlePlayerLeave(String connectionId) {
//        String playerName = connectionToPlayerMap.remove(connectionId);
//        if (playerName != null) {
//            if (!gameManager.isGameStarted()) {
//                gameManager.removePlayer(playerName);
//            }
//            broadcastGameState();
//        }
//    }
//
//    public void handleDiceRoll(String connectionId) {
//        String playerName = connectionToPlayerMap.get(connectionId);
//        if (playerName != null && gameManager.isPlayerTurn(playerName)) {
//            int value = gameManager.rollDice();
//            networkHandler.broadcast(new DiceRollResultEvent(value,
//                gameManager.getCurrentPlayer().getColor()));
//        }
//    }
//
//    public boolean handleMove(String connectionId, int pawnIndex, int steps) {
//        String playerName = connectionToPlayerMap.get(connectionId);
//        if (playerName != null && gameManager.movePawn(playerName, pawnIndex, steps)) {
//            broadcastGameState();
//
//            if (gameManager.hasPlayerWon(playerName)) {
//                networkHandler.broadcast(new GameOverEvent(playerName));
//            } else {
//                gameManager.nextTurn();
//            }
//            return true;
//        }
//        return false;
//    }
//
//    private void broadcastGameState() {
//        networkHandler.broadcast(new GameStateUpdateEvent(
//            gameManager.getCurrentPawnPositions(),
//            gameManager.getCurrentPlayer().getColor(),
//            gameManager.getGameState()
//        ));
//    }
//
//    public void handleDisconnection(String connectionId) {
//        handlePlayerLeave(connectionId);
//        sessionManager.handleDisconnection(connectionId);
//    }
//
//    // For connection ID to player name mapping
//    public String getPlayerName(String connectionId) {
//        return connectionToPlayerMap.get(connectionId);
//    }
//}
