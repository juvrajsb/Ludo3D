package ludo.server.state;

import ludo.core.entities.*;
import ludo.core.events.Event;
import ludo.core.events.clientToServer.*;
import ludo.core.game.GameState;
import ludo.core.game.GameManager;
import ludo.core.network.Connection;
import ludo.core.network.MessageListener;
import ludo.core.network.NetworkMessage;
import ludo.server.networking.ServerNetworkHandler;
import ludo.server.session.SessionManager;
import ludo.core.events.serverToClient.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static ludo.server.Server.LOGGER;

public class ServerGameStateManager implements MessageListener {
    private final GameManager gameManager;
    private final ServerNetworkHandler networkHandler;
    private final Map<String, String> connectionToPlayerMap;
    private boolean gameStarted = false;

    public ServerGameStateManager(ServerNetworkHandler networkHandler) {
        this.networkHandler = networkHandler;
        this.gameManager = GameManager.getInstance();
        this.connectionToPlayerMap = new HashMap<>();
    }

    @Override
    public void onMessageReceived(NetworkMessage message) {
        if (message instanceof Event) {
            Event event = (Event) message;
            if(!Objects.equals(event.getType(), "PONG")){LOGGER.info("Received event: " + event.getType() + " from " + event.getConnection().getConnectionID());}

            try {
                switch (event.getType()) {
                    case "JOIN_GAME_REQUEST":
                        JoinGameRequestEvent joinEvent = (JoinGameRequestEvent) event;
                        LOGGER.info("Player attempting to join: " + joinEvent.getPlayerName() +
                            " with color: " + joinEvent.getDesiredColor());
                        handleJoinRequest(joinEvent);
                        break;

                    case "LEAVE_GAME_REQUEST":
                        LOGGER.info("Player leaving: " + event.getConnection().getConnectionID());
                        handleLeaveRequest((LeaveGameRequestEvent) event);
                        break;

                    case "DICE_ROLL_REQUEST":
                        String playerId = event.getConnection().getConnectionID();
                        LOGGER.info("Dice roll requested by: " + playerId);
                        handleDiceRollRequest((DiceRollRequestEvent) event);
                        break;

                    case "MOVE_REQUEST":
                        MoveRequestEvent moveEvent = (MoveRequestEvent) event;
                        LOGGER.info("Move requested - Player: " + event.getConnection().getConnectionID() +
                            " Pawn: " + moveEvent.getPawnIndex() +
                            " Steps: " + moveEvent.getSteps());
                        handleMoveRequest(moveEvent);
                        break;

                    case "START_GAME_REQUEST":
                        LOGGER.info("Game start requested by: " + event.getConnection().getConnectionID());
                        handleStartGameRequest((StartGameRequestEvent) event);
                        break;
                    case "PONG":
                        handlePong(event);
                        break;
                }
            } catch (Exception e) {
                    LOGGER.severe("Error handling event: " + e.getMessage());
                    onConnectionError(e);
            }
        }
    }

    private void handlePong(Event event) {
        String connectionId = event.getConnection().getConnectionID();
        event.getConnection().resetPingFailure();
        LOGGER.fine("Received pong from client: " + connectionId);
    }


    //    private void handleStartGameRequest(StartGameRequestEvent event) {
//        if (gameManager.getPlayers().size() < 2) {
//            networkHandler.sendToClient(
//                event.getConnection().getConnectionID(),
//                new StartGameResponseEvent(Response.NOT_ENOUGH_PLAYERS)
//            );
//            return;
//        }
//
//        gameManager.startGame();
//        networkHandler.broadcast(new GameStartedEvent());
//
//        // Send initial game state
//        broadcastGameState();
//    }
    private void handleStartGameRequest(StartGameRequestEvent event) {
        // First check if sender is the admin/first player
        String connectionId = event.getConnection().getConnectionID();
        if (!isFirstPlayer(connectionId)) {
            LOGGER.warning("Non-admin player attempted to start game: " + connectionId);
            networkHandler.sendToClient(
                connectionId,
                new StartGameResponseEvent(StartGameResponseEvent.Response.NOT_ADMIN)
            );
            return;
        }

        // Check minimum players requirement
        if (gameManager.getPlayers().size() < 2) {
            LOGGER.info("Not enough players to start game");
            networkHandler.sendToClient(
                connectionId,
                new StartGameResponseEvent(StartGameResponseEvent.Response.NOT_ENOUGH_PLAYERS)
            );
            return;
        }

        // Check if game already started
        if (gameManager.isGameStarted()) {
            networkHandler.sendToClient(
                connectionId,
                new StartGameResponseEvent(StartGameResponseEvent.Response.GAME_ALREADY_STARTED)
            );
            return;
        }
        LOGGER.info("Starting game with " + gameManager.getPlayers().size() + " players");
        // Start the game
        gameManager.startGame();

        // Create and broadcast GameStartedEvent with required parameters
        GameStartedEvent gameStartedEvent = new GameStartedEvent(
            gameManager.getPlayers(),                  // List of all players
            gameManager.getCurrentPlayer().getName(),   // Starting player
            gameManager.getPlayers().size()            // Total number of players
        );
        networkHandler.broadcast(gameStartedEvent);

        // Send success response to admin
        networkHandler.sendToClient(
            connectionId,
            new StartGameResponseEvent(StartGameResponseEvent.Response.OK)
        );

        // Broadcast initial game state
        broadcastGameState();

        logGameState();
    }

    private void logGameState() {
        StringBuilder state = new StringBuilder("Current Game State:\n");
        state.append("Players: ").append(gameManager.getPlayers().size()).append("\n");
        for (Player player : gameManager.getPlayers()) {
            state.append("- ").append(player.getName())
                .append(" (").append(player.getColor()).append(")\n");
        }
        state.append("Current Player: ").append(gameManager.getCurrentPlayer().getName());
        LOGGER.info(state.toString());
    }

    private boolean isFirstPlayer(String connectionId) {
        return gameManager.getPlayers().size() > 0 &&
            gameManager.getPlayers().get(0).getName().equals(
                connectionToPlayerMap.get(connectionId)
            );
    }

    private void handleMoveRequest(MoveRequestEvent event) {

    }

    private void handleDiceRollRequest(DiceRollRequestEvent event) {
        String connectionId = event.getConnection().getConnectionID();
        String playerName = connectionToPlayerMap.get(connectionId);
        if (playerName != null && gameManager.isPlayerTurn(playerName)) {
            int value = gameManager.rollDice();
            DiceRollResultEvent resultEvent = new DiceRollResultEvent(
                value,
                gameManager.getCurrentPlayer().getColor()
            );
            networkHandler.broadcast(resultEvent);
        }
    }

    private void handleLeaveRequest(LeaveGameRequestEvent event) {
        String connectionId = event.getConnection().getConnectionID();
        handlePlayerLeave(connectionId);
    }

    private void handlePlayerLeave(String connectionId) {
        String playerName = connectionToPlayerMap.remove(connectionId);
        if (playerName != null) {
            if (!gameManager.isGameStarted()) {
                gameManager.removePlayer(playerName);
            }
            broadcastGameState();
        }
    }

    @Override
    public void onConnectionError(Exception e) {
        LOGGER.severe("Connection error: " + e.getMessage());
    }

//    private void handleJoinRequest(JoinGameRequestEvent event) {
//        String connectionId = event.getConnection().getConnectionID();
//        boolean joined = handlePlayerJoin(
//            connectionId,
//            event.getPlayerName(),
//            event.getDesiredColor()
//        );
//
//        JoinGameResponseEvent response;
//        if (joined) {
//            response = new JoinGameResponseEvent(Response.OK);
//            if (gameManager.getPlayers().size() == 1) {
//                // First player
//                networkHandler.sendToClient(connectionId, new FirstPlayerEvent());
//            }
//        } else {
//            response = new JoinGameResponseEvent(Response.USERNAME_TAKEN);
//        }
//
//        networkHandler.sendToClient(connectionId, response);
//    }

    private boolean handlePlayerJoin(String connectionId, String playerName, String desiredColor) {
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

    private void broadcastGameState() {
        if (!gameStarted) return;

        Player currentPlayer = gameManager.getCurrentPlayer();
        if (currentPlayer == null) {
            LOGGER.warning("Attempting to broadcast game state but no current player");
            return;
        }

        GameStateUpdateEvent stateEvent = new GameStateUpdateEvent(
            gameManager.getCurrentPawnPositions(),
            currentPlayer.getColor(),
            gameManager.getGameState()
        );
        networkHandler.broadcast(stateEvent);
    }

    private boolean isColorAvailable(String desiredColor) {
        return gameManager.getPlayers().stream()
            .noneMatch(p -> p.getColor().equals(desiredColor));
    }

    private boolean isNameOrColorTaken(String playerName, String desiredColor) {
        return gameManager.getPlayers().stream()
            .anyMatch(p -> p.getName().equals(playerName) || p.getColor().equals(desiredColor));
    }

    private void handleJoinRequest(JoinGameRequestEvent event) {
        String connectionId = event.getConnection().getConnectionID();
        String playerName = event.getPlayerName();
        String color = event.getDesiredColor();

        // Validate name and color
        if (isNameOrColorTaken(playerName, color)) {
            networkHandler.sendToClient(connectionId,
                new JoinGameResponseEvent(Response.USERNAME_TAKEN));
            return;
        }

        // Add player
        Player newPlayer = new Player(playerName, color);
        if (gameManager.addPlayer(newPlayer)) {
            connectionToPlayerMap.put(connectionId, playerName);
            LOGGER.info("Player joined successfully: " + event.getPlayerName());

            // Send responses
            networkHandler.sendToClient(connectionId,
                new JoinGameResponseEvent(Response.OK));

            if (gameManager.getPlayers().size() == 1) {
                networkHandler.sendToClient(connectionId,
                    new FirstPlayerEvent());
                LOGGER.info("First player joined: " + event.getPlayerName());
            }

            // Broadcast update
            networkHandler.broadcast(new PlayerJoinedEvent(newPlayer));
            broadcastPlayerList();
        } else {
            LOGGER.warning("Join failed for player: " + event.getPlayerName() +
                " (name/color taken or game full)");
            JoinGameResponseEvent response = new JoinGameResponseEvent(Response.GAME_FULL);
        }
    }
//    private void handleJoinRequest(JoinGameRequestEvent event) {
//        String connectionId = event.getConnection().getConnectionID();
//        String playerName = event.getPlayerName();
//        String desiredColor = event.getDesiredColor();
//
//        if (isNameOrColorTaken(playerName, desiredColor)) {
//            JoinGameResponseEvent response = new JoinGameResponseEvent(Response.USERNAME_TAKEN);
//            networkHandler.sendToClient(connectionId, response);
//            return;
//        }
//
//        if (gameManager.getPlayers().size() >= 4 || gameManager.isGameStarted()) {
//            JoinGameResponseEvent response = new JoinGameResponseEvent(Response.GAME_FULL);
//            networkHandler.sendToClient(connectionId, response);
//            return;
//        }
//
//        Player newPlayer = new Player(playerName, desiredColor);
//        if (gameManager.addPlayer(newPlayer)) {
//            connectionToPlayerMap.put(connectionId, playerName);
//
//            // Send success response to joining player
//            networkHandler.sendToClient(connectionId, new JoinGameResponseEvent(Response.OK));
//
//            // Notify all players about the new join
//            PlayerJoinedEvent joinEvent = new PlayerJoinedEvent(newPlayer);
//            networkHandler.broadcast(joinEvent);
//
//            // Update waiting room for all players
//            WaitingRoomUpdateEvent updateEvent = new WaitingRoomUpdateEvent(
//                gameManager.getPlayers().stream().map(Player::getName).collect(Collectors.toList()),
//                gameManager.getPlayers().size()
//            );
//            networkHandler.broadcast(updateEvent);
//
//            // Check if we can start the game
//            if (gameManager.getPlayers().size() >= 2) {
//                checkGameStart();
//            }
//        }
//    }
    private void broadcastPlayerList() {
        List<String> playerNames = gameManager.getPlayers().stream()
            .map(Player::getName)
            .collect(Collectors.toList());

        networkHandler.broadcast(new WaitingRoomUpdateEvent(
            playerNames,
            gameManager.getPlayers().stream()
                .collect(Collectors.toMap(Player::getName, Player::getColor)),
            gameManager.getPlayers().size())
        );
    }
    private void checkGameStart() {
        if (gameManager.getPlayers().size() >= 2 && !gameManager.isGameStarted()) {
            // Automatically start game with 4 players
            if (gameManager.getPlayers().size() == 4) {
                startGame();
            }
        }
    }

    private void startGame() {
        gameManager.startGame();

        GameStartedEvent gameStartEvent = new GameStartedEvent(
            gameManager.getPlayers(),
            gameManager.getCurrentPlayer().getName(),
            gameManager.getPlayers().size()
        );
        networkHandler.broadcast(gameStartEvent);

        // Start first turn
        TurnChangeEvent turnEvent = new TurnChangeEvent(gameManager.getCurrentPlayer().getName());
        networkHandler.broadcast(turnEvent);

        // Send initial game state
        broadcastGameState();
    }
}

//package ludo.server.state;
//
//import ludo.core.entities.*;
//import ludo.core.game.GameState;
//import ludo.core.game.GameManager;
//import ludo.core.network.Connection;
//import ludo.server.networking.ServerNetworkHandler;
//import ludo.server.session.SessionManager;
//import ludo.core.events.serverToClient.*;
//
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.logging.Logger;
//
//public class ServerGameStateManager {
//    private static final Logger LOGGER = Logger.getLogger(ServerGameStateManager.class.getName());
//
//    private final GameManager gameManager;
//    private final SessionManager sessionManager;
//    private final ServerNetworkHandler networkHandler;
//    private final Map<String, String> connectionToPlayerMap; // Maps connectionIds to playerNames
//
//    public ServerGameStateManager(ServerNetworkHandler networkHandler) {
//        this.gameManager = GameManager.getInstance();
//        this.sessionManager = SessionManager.getInstance();
//        this.networkHandler = networkHandler;
//        this.connectionToPlayerMap = new ConcurrentHashMap<>();
//    }
//
//    // Player Management
//    public boolean handlePlayerJoin(String connectionId, String playerName, String desiredColor) {
//        if (gameManager.getPlayers().size() >= 4 || gameManager.isGameStarted()) {
//            return false;
//        }
//
//        // Check if color is available
//        if (!isColorAvailable(desiredColor)) {
//            return false;
//        }
//
//        Player newPlayer = new Player(playerName, desiredColor);
//        if (gameManager.addPlayer(newPlayer)) {
//            connectionToPlayerMap.put(connectionId, playerName);
//
//            // Notify other players
//            PlayerJoinedEvent joinEvent = new PlayerJoinedEvent(newPlayer);
//            networkHandler.broadcast(joinEvent);
//
//            // Send updated game state
//            broadcastGameState();
//            return true;
//        }
//        return false;
//    }
//
//    public void handlePlayerLeave(String connectionId) {
//        String playerName = connectionToPlayerMap.get(connectionId);
//        if (playerName != null) {
//            connectionToPlayerMap.remove(connectionId);
//
//            // Notify other players
//            PlayerLeftEvent leaveEvent = new PlayerLeftEvent(playerName);
//            networkHandler.broadcast(leaveEvent);
//
//            if (!gameManager.isGameStarted()) {
//                // Only remove player if game hasn't started
//                gameManager.removePlayer(playerName);
//            }
//            broadcastGameState();
//        }
//    }
//
//    // Game Actions
//    public void handleDiceRoll(String connectionId) {
//        String playerName = connectionToPlayerMap.get(connectionId);
//        if (playerName != null && gameManager.isPlayerTurn(playerName)) {
//            int value = gameManager.rollDice();
//            DiceRollResultEvent event = new DiceRollResultEvent(
//                value,
//                gameManager.getCurrentPlayer().getColor()
//            );
//            networkHandler.broadcast(event);
//        }
//    }
//
//    public boolean handleMove(String connectionId, int pawnIndex, int steps) {
//        String playerName = connectionToPlayerMap.get(connectionId);
//        if (!gameManager.isPlayerTurn(playerName)) {
//            return false;
//        }
//
//        boolean moveSuccess = gameManager.movePawn(playerName, pawnIndex, steps);
//
//        // Create and broadcast move result
//        MoveResultEvent resultEvent = new MoveResultEvent(
//            moveSuccess,
//            moveSuccess ? "Move successful" : "Invalid move",
//            pawnIndex,
//            moveSuccess ? gameManager.getPawnPosition(playerName, pawnIndex) : -1
//        );
//        networkHandler.broadcast(resultEvent);
//
//        if (moveSuccess) {
//            // Check win condition
//            if (gameManager.hasPlayerWon(playerName)) {
//                handleGameOver(playerName);
//            } else {
//                gameManager.nextTurn();
//                broadcastGameState();
//            }
//        }
//
//        return moveSuccess;
//    }
//
//    // State Management
//    private void broadcastGameState() {
//        GameStateUpdateEvent stateEvent = new GameStateUpdateEvent(
//            gameManager.getCurrentPawnPositions(),
//            gameManager.getCurrentPlayer().getColor(),
//            gameManager.getGameState()
//        );
//        networkHandler.broadcast(stateEvent);
//    }
//
//    private void handleGameOver(String winner) {
//        GameOverEvent gameOverEvent = new GameOverEvent(winner);
//        networkHandler.broadcast(gameOverEvent);
//        // Update game state
//        gameManager.setGameState(GameState.GAME_OVER);
//        broadcastGameState();
//    }
//
//    // Connection Management
//    public void handleDisconnection(String connectionId) {
//        handlePlayerLeave(connectionId);
//        sessionManager.handleDisconnection(connectionId);
//    }
//
//    public void handleReconnection(String connectionId, Connection connection) {
//        String playerName = connectionToPlayerMap.get(connectionId);
//        if (playerName != null) {
//            sessionManager.handleReconnection(connection);
//            // Send current game state to reconnected player
//            GameStateUpdateEvent stateEvent = new GameStateUpdateEvent(
//                gameManager.getCurrentPawnPositions(),
//                gameManager.getCurrentPlayer().getColor(),
//                gameManager.getGameState()
//            );
//            networkHandler.sendToClient(connectionId, stateEvent);
//        }
//    }
//
//    // Utility Methods
//    private boolean isColorAvailable(String color) {
//        return gameManager.getPlayers().stream()
//            .noneMatch(p -> p.getColor().equals(color));
//    }
//
//    public boolean isPlayerTurn(String connectionId) {
//        String playerName = connectionToPlayerMap.get(connectionId);
//        return playerName != null && gameManager.isPlayerTurn(playerName);
//    }
//
//    public GameState getGameState() {
//        return gameManager.getGameState();
//    }
//
//    public boolean isGameStarted() {
//        return gameManager.isGameStarted();
//    }
//
//    public String getPlayerName(String connectionId) {
//        return connectionToPlayerMap.get(connectionId);
//    }
//}
//
////package ludo.server.state;
////
////import ludo.core.entities.Player;
////import ludo.core.game.GameManager;
////import ludo.core.game.GameState;
////import ludo.server.networking.ServerNetworkHandler;
////import ludo.server.session.SessionManager;
////import ludo.core.events.serverToClient.*;
////
////    import java.util.Map;
////import java.util.concurrent.ConcurrentHashMap;
////import java.util.logging.Logger;
////
////public class ServerGameStateManager {
////    private static final Logger LOGGER = Logger.getLogger(ServerGameStateManager.class.getName());
////
////    private final GameManager gameManager;
////    private final SessionManager sessionManager;
////    private final ServerNetworkHandler networkHandler;
////    private final Map<String, String> connectionToPlayerMap;
////
////    public ServerGameStateManager(ServerNetworkHandler networkHandler) {
////        this.gameManager = GameManager.getInstance();
////        this.sessionManager = SessionManager.getInstance();
////        this.networkHandler = networkHandler;
////        this.connectionToPlayerMap = new ConcurrentHashMap<>();
////    }
////
////    // Only handle the mapping between connections and game state
////    public boolean handlePlayerJoin(String connectionId, String playerName, String color) {
////        Player newPlayer = new Player(playerName, color);
////        if (gameManager.addPlayer(newPlayer)) {
////            connectionToPlayerMap.put(connectionId, playerName);
////            broadcastGameState();
////            return true;
////        }
////        return false;
////    }
////
////    public void handlePlayerLeave(String connectionId) {
////        String playerName = connectionToPlayerMap.remove(connectionId);
////        if (playerName != null) {
////            if (!gameManager.isGameStarted()) {
////                gameManager.removePlayer(playerName);
////            }
////            broadcastGameState();
////        }
////    }
////
////    public void handleDiceRoll(String connectionId) {
////        String playerName = connectionToPlayerMap.get(connectionId);
////        if (playerName != null && gameManager.isPlayerTurn(playerName)) {
////            int value = gameManager.rollDice();
////            networkHandler.broadcast(new DiceRollResultEvent(value,
////                gameManager.getCurrentPlayer().getColor()));
////        }
////    }
////
////    public boolean handleMove(String connectionId, int pawnIndex, int steps) {
////        String playerName = connectionToPlayerMap.get(connectionId);
////        if (playerName != null && gameManager.movePawn(playerName, pawnIndex, steps)) {
////            broadcastGameState();
////
////            if (gameManager.hasPlayerWon(playerName)) {
////                networkHandler.broadcast(new GameOverEvent(playerName));
////            } else {
////                gameManager.nextTurn();
////            }
////            return true;
////        }
////        return false;
////    }
////
////    private void broadcastGameState() {
////        networkHandler.broadcast(new GameStateUpdateEvent(
////            gameManager.getCurrentPawnPositions(),
////            gameManager.getCurrentPlayer().getColor(),
////            gameManager.getGameState()
////        ));
////    }
////
////    public void handleDisconnection(String connectionId) {
////        handlePlayerLeave(connectionId);
////        sessionManager.handleDisconnection(connectionId);
////    }
////
////    // For connection ID to player name mapping
////    public String getPlayerName(String connectionId) {
////        return connectionToPlayerMap.get(connectionId);
////    }
////}
