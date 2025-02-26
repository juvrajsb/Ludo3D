package ludo.server.state;

import ludo.core.entities.*;
import ludo.core.events.Event;
import ludo.core.events.clientToServer.*;
import ludo.core.game.GameState;
import ludo.core.game.GameManager;
import ludo.core.network.MessageListener;
import ludo.core.network.NetworkMessage;
import ludo.core.utils.Constants;
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

        // ENHANCED VALIDATION: More detailed checks
        if (playerName == null) {
            LOGGER.warning("Dice roll rejected - player not found");
            sendErrorResponse(connectionId, "Player not found");
            return;
        }

        if (gameManager.getGameState() != GameState.IN_PROGRESS) {
            LOGGER.warning("Dice roll rejected - game not in progress");
            sendErrorResponse(connectionId, "Game not in progress");
            return;
        }

        if (!gameManager.isPlayerTurn(playerName)) {
            LOGGER.warning("Dice roll rejected - not player's turn");
            sendErrorResponse(connectionId, "Not your turn");
            return;
        }

        // Check if player has already rolled and needs to move
        if (gameManager.getLastDiceRoll() > 0) {
            LOGGER.warning("Dice roll rejected - player has already rolled");
            sendErrorResponse(connectionId, "You have already rolled");
            return;
        }

        int diceValue = gameManager.rollDice();
        LOGGER.info(String.format("Player %s rolled %d", playerName, diceValue));

        // Update game state
        gameManager.setGameState(GameState.DICE_ROLLED);

        // Send dice result to all players
        DiceRollResultEvent resultEvent = new DiceRollResultEvent(
            diceValue,
            gameManager.getCurrentPlayer().getColor()
        );
        networkHandler.broadcast(resultEvent);

        // If no valid moves are possible with this roll, automatically end turn
        if (!gameManager.hasValidMovesAvailable(gameManager.getCurrentPlayer(), diceValue)) {
            LOGGER.info("No valid moves available - automatically ending turn");

            // Create a proper Timer instance
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    handleTurnEnd(event);
                    timer.cancel(); // Clean up timer
                }
            }, 1500); // 1.5 second delay
        } else {
            // Set state to waiting for move
            gameManager.setGameState(GameState.WAITING_FOR_MOVE);
            broadcastGameState();
        }
    }

    // Helper method to send error responses
    private void sendErrorResponse(String connectionId, String message) {
        ErrorEvent errorEvent = new ErrorEvent(message);
        networkHandler.sendToClient(connectionId, errorEvent);
    }

    private void handleMoveRequest(MoveRequestEvent event) {
        String connectionId = event.getConnection().getConnectionID();
        String playerName = connectionToPlayerMap.get(connectionId);

        LOGGER.info(String.format("Move request from %s (connection: %s)", playerName, connectionId));

        // ENHANCED VALIDATION: Check if game is in progress
        if (gameManager.getGameState() != GameState.IN_PROGRESS &&
            gameManager.getGameState() != GameState.WAITING_FOR_MOVE) {
            LOGGER.warning("Move rejected - game not in proper state: " + gameManager.getGameState());
            sendMoveResponse(connectionId, false, "Game not in proper state", event.getPawnIndex(), -1);
            return;
        }

        // Validate it's the player's turn
        if (!gameManager.isPlayerTurn(playerName)) {
            LOGGER.warning("Move rejected - not player's turn");
            sendMoveResponse(connectionId, false, "Not your turn", event.getPawnIndex(), -1);
            return;
        }

        Player currentPlayer = gameManager.getPlayerByName(playerName);
        if (currentPlayer == null) {
            LOGGER.severe("Player not found in game manager: " + playerName);
            sendMoveResponse(connectionId, false, "Player not found", event.getPawnIndex(), -1);
            return;
        }

        int pawnIndex = event.getPawnIndex();
        int steps = event.getSteps();

        // ENHANCED VALIDATION: Check if pawn index is valid
        if (pawnIndex < 0 || pawnIndex >= Constants.PAWNS_PER_PLAYER) {
            LOGGER.warning("Invalid pawn index: " + pawnIndex);
            sendMoveResponse(connectionId, false, "Invalid pawn index", pawnIndex, -1);
            return;
        }

        // ENHANCED VALIDATION: Check if steps match last dice roll
        if (steps != gameManager.getLastDiceRoll()) {
            LOGGER.warning(String.format("Move rejected - steps (%d) don't match dice roll (%d)",
                steps, gameManager.getLastDiceRoll()));
            sendMoveResponse(connectionId, false, "Invalid move: Dice roll mismatch", pawnIndex, -1);
            return;
        }

        int playerIndex = gameManager.getPlayers().indexOf(currentPlayer);

        // Log before state
        Map<String, List<Integer>> beforePositions = gameManager.getCurrentPawnPositions();
        LOGGER.info("Positions before move: " + beforePositions);

        // ENHANCED: Get pawn details for better logging
        Pawn selectedPawn = currentPlayer.getPawns().get(pawnIndex);
        boolean isLeavingHome = selectedPawn.isHome() && steps == 6;
        int currentPosition = selectedPawn.isHome() ? -1 : selectedPawn.getPosition();

        // Delegate to GameManager for actual move validation and execution
        boolean moveSuccess = gameManager.movePawn(playerIndex, pawnIndex, steps);
        LOGGER.info("Move result: " + (moveSuccess ? "Success" : "Failed"));

        if (moveSuccess) {
            Map<String, List<Integer>> afterPositions = gameManager.getCurrentPawnPositions();
            LOGGER.info("Positions after move: " + afterPositions);
            int newPosition = currentPlayer.getPawns().get(pawnIndex).getPosition();

            // ENHANCED LOGGING: Include more details about the move
            LOGGER.info(String.format("Pawn moved from %d to %d (isLeavingHome: %b)",
                currentPosition, newPosition, isLeavingHome));

            // Send successful move response
            MoveResultEvent resultEvent = new MoveResultEvent(
                true,
                "Move successful",
                pawnIndex,
                newPosition
            );

            // Broadcast to all players
            networkHandler.broadcast(resultEvent);

            // Update game state
            gameManager.setGameState(GameState.PLAYER_MOVED);
            broadcastGameState();

            // Check for win condition
            if (gameManager.hasPlayerWon(playerName)) {
                handleGameOver(playerName);
                return;
            }

            // Handle turn logic - delegated to a dedicated method for clarity
            handleTurnLogicAfterMove(steps, isLeavingHome, currentPlayer);
        } else {
            LOGGER.warning("Move failed for pawn " + pawnIndex);
            sendMoveResponse(connectionId, false, "Invalid move", pawnIndex, -1);
        }
    }

    private void handleTurnLogicAfterMove(int steps, boolean isLeavingHome, Player currentPlayer) {
        if (steps == 6) {
            // If player rolled a 6, they get another turn
            if (isLeavingHome) {
                LOGGER.info("Pawn left home with a 6 - player gets another roll");
                // Don't end turn, just let them roll again
                gameManager.setGameState(GameState.IN_PROGRESS);
            } else {
                LOGGER.info("Player moved with a 6 - player gets another roll");
                // Don't end turn, let them roll again
                gameManager.setGameState(GameState.IN_PROGRESS);
            }

            // In both cases, we need to broadcast that the player can roll again
            networkHandler.sendToClient(
                gameManager.getCurrentPlayer().getName(),
                new CanRollAgainEvent()  // We'll need to create this event
            );
        } else {
            // Move to next player's turn
            gameManager.nextTurn();
            String nextPlayer = gameManager.getCurrentPlayer().getName();
            LOGGER.info("Next player: " + nextPlayer);

            // Send turn change event
            TurnChangeEvent turnEvent = new TurnChangeEvent(nextPlayer);
            networkHandler.broadcast(turnEvent);
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

            // Check if the next player is a bot
            checkAndPlayBotTurn();
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
        int realPlayerCount = gameManager.getPlayers().size();
        boolean enableBots = event.isBotsEnabled();

        if (realPlayerCount < 2 && !enableBots) {
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

        LOGGER.info("Starting game with " + realPlayerCount + " real players" +
            (enableBots ? " and bot players" : ""));

        // Add bot players if needed and enabled
        if (enableBots) {
            addBotPlayers();
        }

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

        // Start bot turn if current player is a bot
        checkAndPlayBotTurn();

        logGameState();
    }

    private void addBotPlayers() {
        List<String> availableColors = new ArrayList<>(Arrays.asList("RED", "BLUE", "GREEN", "YELLOW"));

        // Remove colors already in use by real players
        for (Player player : gameManager.getPlayers()) {
            availableColors.remove(player.getColor().toUpperCase());
        }

        // Add bots until we have 4 players or run out of colors
        int currentPlayerCount = gameManager.getPlayers().size();
        int botsToAdd = Math.min(Constants.MAX_PLAYERS - currentPlayerCount, availableColors.size());

        for (int i = 0; i < botsToAdd; i++) {
            String botColor = availableColors.get(i);
            String botName = "Bot-" + botColor;

            // Create bot player and add to game
            BotPlayer bot = new BotPlayer(botName, botColor);
            gameManager.addPlayer(bot);

            LOGGER.info("Added bot player: " + botName + " (" + botColor + ")");
        }
    }

    private void checkAndPlayBotTurn() {
        Player currentPlayer = gameManager.getCurrentPlayer();
        if (currentPlayer instanceof BotPlayer) {
            // Use a thread with delay to simulate bot "thinking"
            new Thread(() -> {
                try {
                    // Small delay before bot makes a move
                    Thread.sleep(1500);
                    playBotTurn((BotPlayer) currentPlayer);
                } catch (InterruptedException e) {
                    LOGGER.warning("Bot turn interrupted: " + e.getMessage());
                }
            }).start();
        }
    }

    private void playBotTurn(BotPlayer bot) {
        LOGGER.info("Bot " + bot.getName() + " taking turn");

        // Roll dice for bot
        int diceRoll = gameManager.rollDice();
        LOGGER.info("Bot rolled: " + diceRoll);

        // Broadcast dice roll event
        DiceRollResultEvent diceEvent = new DiceRollResultEvent(
            diceRoll,
            bot.getColor()
        );
        networkHandler.broadcast(diceEvent);

        // Let bot choose best move
        int playerIndex = gameManager.getPlayers().indexOf(bot);
        int pawnIndex = bot.chooseBestMove(gameManager.getBoard(), diceRoll, gameManager.getPlayers());

        if (pawnIndex >= 0) {
            // Execute bot's chosen move
            LOGGER.info("Bot moving pawn " + pawnIndex + " with roll " + diceRoll);

            boolean moveSuccess = gameManager.movePawn(playerIndex, pawnIndex, diceRoll);
            if (moveSuccess) {
                // Broadcast move result
                Pawn movedPawn = bot.getPawns().get(pawnIndex);
                MoveResultEvent moveEvent = new MoveResultEvent(
                    true,
                    "Bot moved successfully",
                    pawnIndex,
                    movedPawn.getPosition()
                );
                networkHandler.broadcast(moveEvent);

                // Update game state
                broadcastGameState();

                // Check for win condition
                if (gameManager.hasPlayerWon(bot.getName())) {
                    handleGameOver(bot.getName());
                    return;
                }
            } else {
                LOGGER.warning("Bot move failed");
            }
        } else {
            LOGGER.info("Bot has no valid moves");
        }

        // Handle turn logic based on dice roll
        if (diceRoll == 6) {
            // Bot gets another turn
            try {
                Thread.sleep(1000); // Brief pause before next bot action
                playBotTurn(bot);
            } catch (InterruptedException e) {
                LOGGER.warning("Bot turn interrupted: " + e.getMessage());
            }
        } else {
            // Move to next player
            gameManager.nextTurn();

            // Send turn change event
            TurnChangeEvent turnEvent = new TurnChangeEvent(
                gameManager.getCurrentPlayer().getName()
            );
            networkHandler.broadcast(turnEvent);

            // Check if next player is also a bot
            checkAndPlayBotTurn();
        }
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
