package ludo.server.state;

import ludo.core.entities.*;
import ludo.core.events.Event;
import ludo.core.events.clientToServer.*;
import ludo.core.game.GameState;
import ludo.core.game.GameManager;
import ludo.core.network.MessageListener;
import ludo.core.network.NetworkMessage;
import ludo.server.networking.ServerNetworkHandler;
import ludo.core.events.serverToClient.*;

import java.util.*;
import java.util.stream.Collectors;

import static ludo.server.Server.LOGGER;

public class ServerGameStateManager implements MessageListener {
    private final GameManager gameManager;
    private final ServerNetworkHandler networkHandler;
    private final Map<String, String> connectionToPlayerMap;

    public ServerGameStateManager(ServerNetworkHandler networkHandler) {
        this.networkHandler = networkHandler;
        this.gameManager = GameManager.getInstance();
        this.connectionToPlayerMap = new HashMap<>();
    }

    @Override
    public void onMessageReceived(NetworkMessage message) {
        if (message instanceof Event event) {
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
                    case "TURN_END":
                        LOGGER.info("Turn end event received from: " + event.getConnection().getConnectionID());
                        handleTurnEnd(event);
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

    private void handleDiceRollRequest(DiceRollRequestEvent event) {
        String connectionId = event.getConnection().getConnectionID();
        String playerName = connectionToPlayerMap.get(connectionId);

        if (playerName != null && gameManager.isPlayerTurn(playerName)) {
            int diceValue = gameManager.rollDice();
            LOGGER.info(String.format("Player %s rolled %d", playerName, diceValue));

            // Send dice result to all players
            DiceRollResultEvent resultEvent = new DiceRollResultEvent(
                diceValue,
                gameManager.getCurrentPlayer().getColor()
            );
            networkHandler.broadcast(resultEvent);

            // If no valid moves are possible with this roll, automatically end turn
            if (!gameManager.hasValidMovesAvailable(gameManager.getCurrentPlayer(), diceValue)) { //todo has valid moves
                LOGGER.info("No valid moves available - automatically ending turn");
                handleTurnEnd(event);
            }
        }
    }

    private void handleMoveRequest(MoveRequestEvent event) {
        String connectionId = event.getConnection().getConnectionID();
        String playerName = connectionToPlayerMap.get(connectionId);

        LOGGER.info(String.format("Move request from %s (connection: %s)", playerName, connectionId));
        LOGGER.info(String.format("Current server player is: %s",
            gameManager.getCurrentPlayer().getName()));
        LOGGER.info("Is player's turn? " + gameManager.isPlayerTurn(playerName));

        // Validate it's the player's turn
        if (!gameManager.isPlayerTurn(playerName)) {
            LOGGER.warning("Move rejected - not player's turn");
            sendMoveResponse(connectionId, false, "Not your turn", event.getPawnIndex(), -1);
            return;
        }

        Player currentPlayer = gameManager.getPlayerByName(playerName);
        if (currentPlayer == null) {
            LOGGER.severe("Player not found in game manager: " + playerName);
            return;
        }

        int pawnIndex = event.getPawnIndex();
        int steps = event.getSteps();
        int playerIndex = gameManager.getPlayers().indexOf(currentPlayer);

        LOGGER.info(String.format("Move parameters - PlayerIndex: %d, PawnIndex: %d, Steps: %d",
            playerIndex, pawnIndex, steps));

        Map<String, List<Integer>> beforePositions = gameManager.getCurrentPawnPositions();
        LOGGER.info("Positions before move: " + beforePositions);

        Pawn selectedPawn = currentPlayer.getPawns().get(pawnIndex);
        boolean isLeavingHome = selectedPawn.isHome() && steps == 6;
        boolean moveSuccess = gameManager.movePawn(playerIndex, pawnIndex, steps);
        LOGGER.info("Move result: " + (moveSuccess ? "Success" : "Failed"));

        if (moveSuccess) {
            Map<String, List<Integer>> afterPositions = gameManager.getCurrentPawnPositions();
            LOGGER.info("Positions after move: " + afterPositions);
            int newPosition = currentPlayer.getPawns().get(pawnIndex).getPosition();
            LOGGER.info("New position: " + newPosition);

            // Send successful move response
            MoveResultEvent resultEvent = new MoveResultEvent(
                true,
                "Move successful",
                pawnIndex,
                newPosition
            );
            LOGGER.info("Broadcasting move result: " + resultEvent);
            networkHandler.broadcast(resultEvent);

            // Update game state
            broadcastGameState();

            // Check for win condition
            if (gameManager.hasPlayerWon(playerName)) {
                handleGameOver(playerName);
                return;
            }

            // Handle turn logic
            if (steps == 6) {
                // If player rolled a 6, they get another turn
                if (isLeavingHome) {
                    // If the pawn just left home with a 6, player gets another roll
                    LOGGER.info("Pawn left home with a 6 - player gets another roll");
                    // Don't end turn, just let them roll again
                } else {
                    // If they moved a pawn with a 6, they get another roll
                    LOGGER.info("Player moved with a 6 - player gets another roll");
                    // Don't end turn, let them roll again
                }
            } else {
//                boolean hasValidMove = gameManager.hasValidMovesAvailable(gameManager.getCurrentPlayer(), steps); //todo has valid moves
//                if (!hasValidMove) {
//                    LOGGER.info("No valid moves left - ending turn");
//                    gameManager.nextTurn();
//
//                    // Send turn change event
//                    String nextPlayer = gameManager.getCurrentPlayer().getName();
//                    LOGGER.info("Next player: " + nextPlayer);
////                    TurnChangeEvent turnEvent = new TurnChangeEvent(nextPlayer);
////                    networkHandler.broadcast(turnEvent);
//                } else {
//                    LOGGER.info("Valid moves available - player continues turn");
//                }
                gameManager.nextTurn();
                String nextPlayer = gameManager.getCurrentPlayer().getName();
                LOGGER.info("Next player: " + nextPlayer);
                TurnChangeEvent turnEvent = new TurnChangeEvent(nextPlayer);
                networkHandler.broadcast(turnEvent);
            }
        } else {
            LOGGER.warning("Move failed for pawn " + pawnIndex);
            sendMoveResponse(connectionId, false, "Invalid move", pawnIndex, -1);
        }
    }

    private void handleTurnEnd(Event event) {
        String connectionId = event.getConnection().getConnectionID();
        String playerName = connectionToPlayerMap.get(connectionId);

        if (playerName != null && gameManager.isPlayerTurn(playerName)) {
            LOGGER.info("Turn ended for player: " + playerName);

            // Move to next player
            gameManager.nextTurn();

            // Send turn change event
            TurnChangeEvent turnEvent = new TurnChangeEvent(
                gameManager.getCurrentPlayer().getName()
            );
            networkHandler.broadcast(turnEvent);

            // Update game state
            broadcastGameState();
        }
    }

    private void broadcastGameState() {
        GameStateUpdateEvent stateEvent = new GameStateUpdateEvent(
            gameManager.getCurrentPawnPositions(),
            gameManager.getCurrentPlayer().getColor(),
            gameManager.getGameState()
        );
        networkHandler.broadcast(stateEvent);
    }

    private void sendMoveResponse(String connectionId, boolean success, String message,
                                  int pawnIndex, int newPosition) {
        MoveResultEvent response = new MoveResultEvent(success, message, pawnIndex, newPosition);
        networkHandler.sendToClient(connectionId, response);
    }

    private void handlePong(Event event) {
        String connectionId = event.getConnection().getConnectionID();
        event.getConnection().resetPingFailure();
        LOGGER.fine("Received pong from client: " + connectionId);
    }

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

    private void handleGameOver(String winner) {
        GameOverEvent gameOverEvent = new GameOverEvent(winner);
        networkHandler.broadcast(gameOverEvent);
        gameManager.setGameState(GameState.GAME_OVER);
        broadcastGameState();
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

    private boolean isColorAvailable(String desiredColor) {
        return gameManager.getPlayers().stream()
            .noneMatch(p -> p.getColor().equals(desiredColor));
    }

    private boolean isNameTaken(String playerName, String desiredColor) {
        return gameManager.getPlayers().stream()
            .anyMatch(p -> p.getName().equals(playerName));
    }

    private boolean isColorTaken(String desiredColor) {
        return gameManager.getPlayers().stream()
            .anyMatch(p -> p.getColor().equals(desiredColor.toUpperCase()));
    }

    private void handleJoinRequest(JoinGameRequestEvent event) {
        String connectionId = event.getConnection().getConnectionID();
        String playerName = event.getPlayerName();
        String color = event.getDesiredColor().toUpperCase();

        // Validate name
        if (isNameTaken(playerName.toUpperCase(), color.toUpperCase())) {
            networkHandler.sendToClient(connectionId,
                new JoinGameResponseEvent(Response.USERNAME_TAKEN));
            return;
        }
        // Validate color
        if (isColorTaken(color.toUpperCase())) {
            networkHandler.sendToClient(connectionId,
                new JoinGameResponseEvent(Response.COLOR_TAKEN));
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

//    private void checkGameStart() {
//        if (gameManager.getPlayers().size() >= 2 && !gameManager.isGameStarted()) {
//            // Automatically start game with 4 players
//            if (gameManager.getPlayers().size() == 4) {
//                startGame();
//            }
//        }
//    }

//    private void startGame() {
//        gameManager.startGame();
//
//        GameStartedEvent gameStartEvent = new GameStartedEvent(
//            gameManager.getPlayers(),
//            gameManager.getCurrentPlayer().getName(),
//            gameManager.getPlayers().size()
//        );
//        networkHandler.broadcast(gameStartEvent);
//
//        // Start first turn
//        TurnChangeEvent turnEvent = new TurnChangeEvent(gameManager.getCurrentPlayer().getName());
//        LOGGER.info("Starting game with player: " + gameManager.getCurrentPlayer().getName());
//        networkHandler.broadcast(turnEvent);
//
//        // Send initial game state
//        broadcastGameState();
//    }
}
