package ludo.server.state;

import ludo.core.entities.*;
import ludo.core.events.Event;
import ludo.core.events.clientToServer.*;
import ludo.core.game.GameState;
import ludo.core.game.GameManager;
import ludo.core.network.MessageListener;
import ludo.core.network.NetworkMessage;
import ludo.core.persistence.GamePersistence;
import ludo.core.utils.Constants;
import ludo.core.validation.MoveValidator;
import ludo.server.networking.ServerNetworkHandler;
import ludo.core.events.serverToClient.*;
import ludo.core.network.Connection;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static ludo.server.Server.LOGGER;

public class ServerGameStateManager implements MessageListener {
    private final GameManager gameManager;
    private final ServerNetworkHandler networkHandler;
    private final Map<String, String> connectionToPlayerMap;
    private static final String[] COLOR_ORDER = {"YELLOW", "BLUE", "RED", "GREEN"};
    private String adminPlayerName = null;

    public ServerGameStateManager(ServerNetworkHandler networkHandler) {
        this.networkHandler = networkHandler;
        this.gameManager = GameManager.getInstance();
        this.connectionToPlayerMap = new HashMap<>();
    }

    @Override
    public void onMessageReceived(NetworkMessage message) {
        if (message instanceof Event event) {
            String clientId = event.getConnection() != null ? event.getConnection().getConnectionID() : null;
            if (!Objects.equals(event.getType(), "PONG")) {
                LOGGER.info("Received event: " + event.getType () + (clientId != null ? " from " + clientId : ""));
            }
            if (event instanceof ClientDisconnectedEvent) { //take this in the switch todo
                handleClientDisconnection(event.getConnection());
            }

            try {
                switch (event.getType()) {
                    case "JOIN_GAME_REQUEST":
                        JoinGameRequestEvent joinEvent = (JoinGameRequestEvent) event;
                        handleJoinRequest(joinEvent);
                        break;

                    case "LEAVE_GAME_REQUEST":
                        LOGGER.info("Player leaving: " + event.getConnection().getConnectionID());
                        handleLeaveRequest((LeaveGameRequestEvent) event);
                        break;

                    case "DICE_ROLL_REQUEST":
                        handleDiceRollRequest((DiceRollRequestEvent) event);
                        break;

                    case "MOVE_REQUEST":
                        MoveRequestEvent moveEvent = (MoveRequestEvent) event;
                        handleMoveRequest(moveEvent);
                        break;

                    case "START_GAME_REQUEST":
                        handleStartGameRequest((StartGameRequestEvent) event);
                        break;

                    case "TURN_END":
                        handleTurnEnd(event);
                        break;

                    case "PONG":
                        handlePong(event);
                        break;

                    case "SAVE_GAME_REQUEST":
                        handleSaveGameRequest((SaveGameRequestEvent) event);
                        break;

                    case "LOAD_GAME_REQUEST":
                        handleLoadGameRequest((LoadGameRequestEvent) event);
                        break;

                    case "REQUEST_SAVE_FILES":
                        handleRequestSaveFiles((RequestSaveFilesEvent) event);
                        break;
                    case "RECONNECT_REQUEST":
                        handleReconnectRequest((ReconnectRequestEvent) event);
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

        if (gameManager.getLastDiceRoll() > 0) {
            LOGGER.warning("Dice roll rejected - player has already rolled");
            sendErrorResponse(connectionId, "You have already rolled");
            return;
        }

        int diceValue = gameManager.rollDice();
//        LOGGER.info(String.format("Player %s rolled %d", playerName, diceValue));

        gameManager.setGameState(GameState.DICE_ROLLED);

        DiceRollResultEvent resultEvent = new DiceRollResultEvent(
            diceValue,
            gameManager.getCurrentPlayer().getColor()
        );
        networkHandler.broadcast(resultEvent);

        // If no valid moves are possible with this roll, automatically end turn
        if (!gameManager.hasValidMovesAvailable(gameManager.getCurrentPlayer(), diceValue)) {
            LOGGER.info("No valid moves available - automatically ending turn");

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    handleTurnEnd(event);
                    timer.cancel(); // Clean up timer
                }
            }, 1500); // 1.5 seconds delay
        } else {
            gameManager.setGameState(GameState.WAITING_FOR_MOVE);
            broadcastGameState();
        }
    }

    private void handleReconnectRequest(ReconnectRequestEvent event) {
        String playerName = event.getPlayerName();
        Connection newConnection = event.getConnection();
        String newConnectionId = newConnection.getConnectionID();

        Player player = gameManager.getPlayerByName(playerName);

        if (player != null && player.isDisconnected()) {
            LOGGER.info("Reconnection successful for player: " + playerName);

            player.setDisconnected(false);
            connectionToPlayerMap.put(newConnectionId, playerName);
            networkHandler.getServer().getClientConnectionHandler().handleReconnection(playerName, newConnection);

            // Send GameStartedEvent to get them back into the game screen
            networkHandler.sendToClient(newConnectionId, new GameStartedEvent(
                gameManager.getPlayers(),
                gameManager.getCurrentPlayer().getName(),
                gameManager.getPlayers().size()
            ));

            // Send the latest game state to sync them up
            networkHandler.sendToClient(newConnectionId, new GameStateUpdateEvent(
                gameManager.getCurrentPawnPositions(),
                gameManager.getCurrentPlayer().getName(),
                gameManager.getGameState()
            ));
        } else {
            sendErrorResponse(newConnectionId, "Could not find a disconnected player with that name.");
            LOGGER.warning("Rejected reconnection attempt for player: " + playerName);
        }
    }

    private void sendErrorResponse(String connectionId, String message) {
        ErrorEvent errorEvent = new ErrorEvent(message);
        networkHandler.sendToClient(connectionId, errorEvent);
    }

    /**
     * This method handles move requests from clients with improved error handling.
     * The key fix is maintaining the proper game state after a failed move.
     */
    public void handleMoveRequest(MoveRequestEvent event) {
        String connectionId = event.getConnection().getConnectionID();
        String playerName = getPlayerNameForConnection(connectionId);

        // Validate game state
        if (gameManager.getGameState() != GameState.IN_PROGRESS &&
            gameManager.getGameState() != GameState.WAITING_FOR_MOVE &&
            gameManager.getGameState() != GameState.PLAYER_MOVED) {
            LOGGER.warning("Move rejected - game not in proper state: " + gameManager.getGameState());
            sendMoveResponse(connectionId, false, "Game not in proper state", event.getPawnIndex(), -1);
            return;
        }

        // Validate player turn
        if (!gameManager.isPlayerTurn(playerName)) {
            LOGGER.warning("Move rejected - not player's turn. Current player: " +
                gameManager.getCurrentPlayer().getColor() + ", Requesting player: " + playerName);
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

        if (pawnIndex < 0 || pawnIndex >= currentPlayer.getPawns().size()) {
            LOGGER.warning("Invalid pawn index: " + pawnIndex);
            sendMoveResponse(connectionId, false, "Invalid pawn index", pawnIndex, -1);
            return;
        }

        if (steps != gameManager.getLastDiceRoll()) {
            LOGGER.warning(String.format("Move rejected - steps (%d) don't match dice roll (%d)",
                steps, gameManager.getLastDiceRoll()));
            sendMoveResponse(connectionId, false, "Invalid move: Dice roll mismatch", pawnIndex, -1);
            return;
        }

        int playerIndex = gameManager.getPlayers().indexOf(currentPlayer);

        Pawn selectedPawn = currentPlayer.getPawns().get(pawnIndex);
        boolean isLeavingHome = selectedPawn.isHome() && steps == 6;
        int currentPosition = selectedPawn.isHome() ? -1 : selectedPawn.getPosition();

        boolean moveSuccess = gameManager.movePawn(playerIndex, pawnIndex, steps);

        if (moveSuccess) {
            handleMoveSuccess(connectionId, pawnIndex, steps, isLeavingHome, currentPosition);
        } else {
            LOGGER.warning("Move failed for pawn " + pawnIndex);

            String failureReason = getFailureReason(selectedPawn, steps);
            sendMoveResponse(connectionId, false, failureReason, pawnIndex, -1);

            gameManager.setGameState(GameState.WAITING_FOR_MOVE);

            broadcastGameState();
        }
    }

    private boolean handleMoveSuccess(String connectionId, int pawnIndex, int steps, boolean isLeavingHome, int currentPosition) {
        int newPosition = gameManager.getCurrentPlayer().getPawns().get(pawnIndex).getPosition();

        MoveResultEvent resultEvent = new MoveResultEvent(
            true, "Move successful", pawnIndex, newPosition);
        networkHandler.broadcast(resultEvent);

        gameManager.setGameState(GameState.PLAYER_MOVED);
        broadcastGameState();

        if (gameManager.hasPlayerWon(gameManager.getCurrentPlayer().getName())) {
            handleGameOver(gameManager.getCurrentPlayer().getName());
            return true;
        }

        // Check for extra roll conditions
        boolean shouldGetExtraRoll = false;
        String reason = "";

        // Check if pawn reached finish base
        if (gameManager.getCurrentPlayer().getPawns().get(pawnIndex).isFinished()) {
            shouldGetExtraRoll = true;
            reason = "Pawn reached finish base";
        }
        // Check if a pawn was captured
        else if (gameManager.wasPawnCaptured()) {
            shouldGetExtraRoll = true;
            reason = "Pawn captured";
        }
        // Check if rolled a 6
        else if (steps == 6) {
            shouldGetExtraRoll = true;
            reason = "Rolled a 6";
        }

        if (shouldGetExtraRoll) {
            LOGGER.info("Player gets another roll: " + reason);
            gameManager.setGameState(GameState.IN_PROGRESS);
            gameManager.setLastDiceRoll(0);

            // Send turn change event to keep the same player
            networkHandler.broadcast(new TurnChangeEvent(gameManager.getCurrentPlayer().getName()));
            networkHandler.sendToClient(connectionId, new CanRollAgainEvent());
        } else {
            gameManager.nextTurn();
            String nextPlayer = gameManager.getCurrentPlayer().getName();
            networkHandler.broadcast(new TurnChangeEvent(nextPlayer));
            checkAndPlayBotTurn();
        }

        broadcastGameState();
        return true;
    }

    private void handleRequestSaveFiles(RequestSaveFilesEvent event) {
        String connectionId = event.getConnection().getConnectionID();
        LOGGER.info("Received request for save files list from " + connectionId);

        // Get the list of save files from the persistence layer
        List<String> saveFiles = GamePersistence.listSaveFiles();

        LOGGER.info("Found " + saveFiles.size() + " save files. Sending list to client.");

        // Send the list back to the client who requested it
        networkHandler.sendToClient(connectionId, new SaveFilesListEvent(saveFiles));
    }

    private String getPlayerNameForConnection(String connectionId) {
        return connectionToPlayerMap.get(connectionId);
    }

    private String getFailureReason(Pawn pawn, int steps) {
        if (pawn.isHome() && steps != 6) {
            return "Cannot leave home without a 6";
        } else if (pawn.isFinished()) {
            return "Pawn has already reached home";
        } else {
            return "Invalid move";
        }
    }

    private void handleTurnEnd(Event event) {
        String connectionId = event.getConnection().getConnectionID();
        String playerName = connectionToPlayerMap.get(connectionId);

        if (playerName != null && gameManager.isPlayerTurn(playerName)) {
            LOGGER.info("Turn ended for player: " + playerName);

            gameManager.nextTurn();
            TurnChangeEvent turnEvent = new TurnChangeEvent(
                gameManager.getCurrentPlayer().getName()
            );
            networkHandler.broadcast(turnEvent);

            broadcastGameState();

            checkAndPlayBotTurn();
        }
    }

    private void broadcastGameState() {
        GameStateUpdateEvent stateEvent = new GameStateUpdateEvent(
            gameManager.getCurrentPawnPositions(),
            gameManager.getCurrentPlayer().getName(),
            gameManager.getGameState()
        );
        networkHandler.broadcast(stateEvent);
    }

    private void sendMoveResponse(String connectionId, boolean success, String message, int pawnIndex, int newPosition) {
        MoveResultEvent response = new MoveResultEvent(success, message, pawnIndex, newPosition);
        networkHandler.sendToClient(connectionId, response);
    }

    private void handlePong(Event event) {
        String connectionId = event.getConnection().getConnectionID();
        event.getConnection().resetPingFailure();
        LOGGER.fine("Received pong from client: " + connectionId);
    }

    private void initializePlayersWithColors(List<Player> realPlayers, boolean enableBots) {
        gameManager.clearPlayers();

        Map<String, Player> playersByColor = new HashMap<>();

        for (Player player : realPlayers) {
            playersByColor.put(player.getColor().toUpperCase(), player);
        }

        if (enableBots) {
            for (String color : COLOR_ORDER) {
                if (!playersByColor.containsKey(color) && playersByColor.size() < Constants.MAX_PLAYERS) {
                    String botName = "Bot-" + color;
                    BotPlayer bot = new BotPlayer(botName, color);
                    playersByColor.put(color, bot);
                    LOGGER.info("Added bot player: " + botName + " (" + color + ")");
                }
            }
        }

        Random random = new Random();
        int startingColorIndex = random.nextInt(COLOR_ORDER.length);
        LOGGER.info("Randomizing starting color. Selected index: " + startingColorIndex +
            " (" + COLOR_ORDER[startingColorIndex] + ")");

        for (int i = 0; i < COLOR_ORDER.length; i++) {
            int colorIndex = (startingColorIndex + i) % COLOR_ORDER.length;
            String color = COLOR_ORDER[colorIndex];

            Player player = playersByColor.get(color);
            if (player != null) {
                gameManager.addPlayer(player);
            }
        }

        LOGGER.info("Final player order:");
        for (int i = 0; i < gameManager.getPlayers().size(); i++) {
            Player p = gameManager.getPlayers().get(i);
            LOGGER.info(i + ": " + p.getName() + " (" + p.getColor() + ")");
        }
    }

    private void handleStartGameRequest(StartGameRequestEvent event) {
        String connectionId = event.getConnection().getConnectionID();
        if (!isFirstPlayer(connectionId)) {
            LOGGER.warning("Non-admin player attempted to start game: " + connectionId);
            networkHandler.sendToClient(
                connectionId,
                new StartGameResponseEvent(StartGameResponseEvent.Response.NOT_ADMIN)
            );
            return;
        }

        boolean enableBots = event.isBotsEnabled();
        boolean loadSavedGame = event.isLoadSavedGame();
        LOGGER.info("Game start request details - Real players: " + gameManager.getPlayers().size() +
            ", Enable bots: " + enableBots +
            ", Load saved game: " + loadSavedGame);

        if (gameManager.isGameStarted()) {
            networkHandler.sendToClient(
                connectionId,
                new StartGameResponseEvent(StartGameResponseEvent.Response.GAME_ALREADY_STARTED)
            );
            return;
        }

        // Attempt to load a saved game first if requested
        if (loadSavedGame) {
            LOGGER.info("Attempting to load saved game");
            String saveFileName = event.getSaveFileName();
            if (saveFileName != null) {
                GamePersistence.GameSaveData saveData = GamePersistence.loadGame(saveFileName);
                if (saveData != null) {
                    LOGGER.info("Successfully loaded save data - Players: " + saveData.players.size() +
                        ", Current color: " + saveData.currentPlayerColor +
                        ", Game state: " + saveData.gameState);

                    gameManager.clearPlayers();
                    loadSavedGameState(saveData, connectionId);

                    GameStartedEvent gameStartedEvent = new GameStartedEvent(
                        gameManager.getPlayers(),
                        gameManager.getCurrentPlayer().getName(),
                        gameManager.getPlayers().size()
                    );
                    networkHandler.broadcast(gameStartedEvent);

                    broadcastGameState();
                    checkAndPlayBotTurn();
                    return;
                }
            }
            LOGGER.warning("Failed to load saved game data, falling back to new game");
        }

        int realPlayerCount = gameManager.getPlayers().size();
        if (realPlayerCount < 2 && !enableBots) {
            LOGGER.info("Not enough players to start a new game");
            networkHandler.sendToClient(
                connectionId,
                new StartGameResponseEvent(StartGameResponseEvent.Response.NOT_ENOUGH_PLAYERS)
            );
            return;
        }

        if (enableBots) {
            addBotPlayers();
        }

        initializePlayersWithColors(gameManager.getPlayers(), enableBots);
        gameManager.startGame();

        GameStartedEvent gameStartedEvent = new GameStartedEvent(
            gameManager.getPlayers(),
            gameManager.getCurrentPlayer().getName(),
            gameManager.getPlayers().size()
        );
        networkHandler.broadcast(gameStartedEvent);

        networkHandler.sendToClient(
            connectionId,
            new StartGameResponseEvent(StartGameResponseEvent.Response.OK)
        );

        broadcastGameState();
        checkAndPlayBotTurn();
        logGameState();
    }

    private void loadSavedGameState(GamePersistence.GameSaveData saveData, String connectionId) {
        gameManager.clearPlayers();

        String loadingPlayerName = getPlayerNameForConnection(connectionId);
        boolean playerSubstituted = false;

        LOGGER.info("Loading game state. The player loading the game is: " + loadingPlayerName);

        // Re-create the players and pawns with the correct types
        for (GamePersistence.PlayerSaveData playerData : saveData.players) {
            String finalPlayerName = playerData.name;
            Player player; // Use a base Player reference

            // Check if the saved player is a bot
            if (playerData.name.startsWith("Bot-")) {
                LOGGER.info("Creating BotPlayer: " + playerData.name);
                player = new BotPlayer(playerData.name, playerData.color);
            } else {
                // This is a human player. Check if we need to substitute the name.
                if (!playerSubstituted) {
                    finalPlayerName = loadingPlayerName;
                    playerSubstituted = true;
                    LOGGER.info("Substituting saved player '" + playerData.name + "' with current player '" + finalPlayerName + "' for color " + playerData.color);
                }
                LOGGER.info("Creating human Player: " + finalPlayerName);
                player = new Player(finalPlayerName, playerData.color);
            }

            // Set the pawn states from the save data
            for (int i = 0; i < playerData.pawns.size(); i++) {
                GamePersistence.PawnSaveData pawnData = playerData.pawns.get(i);
                Pawn pawn = player.getPawns().get(i);
                pawn.setPosition(pawnData.position);
                pawn.setFinished(pawnData.isFinished);
                if (pawnData.isHome) {
                    pawn.sendHome();
                }
            }

            // Add the fully configured player (either Player or BotPlayer) to the game
            gameManager.addPlayer(player);
        }

        // Set the game state from the save file
        gameManager.setGameState(saveData.gameState);

        // Set the correct current player index in the game manager
        for (int i = 0; i < gameManager.getPlayers().size(); i++) {
            if (gameManager.getPlayers().get(i).getColor().equals(saveData.currentPlayerColor)) {
                while(!gameManager.getCurrentPlayer().getColor().equals(saveData.currentPlayerColor)) {
                    gameManager.nextTurn();
                }
                LOGGER.info("Set current turn to player: " + gameManager.getCurrentPlayer().getName() + " (" + gameManager.getCurrentPlayer().getColor() + ")");
                break;
            }
        }

        // Now officially start the game logic
        gameManager.startGame();
    }

    private void addBotPlayers() {
        List<String> availableColors = new ArrayList<>(Arrays.asList("RED", "BLUE", "GREEN", "YELLOW"));

        for (Player player : gameManager.getPlayers()) {
            availableColors.remove(player.getColor().toUpperCase());
        }

        int currentPlayerCount = gameManager.getPlayers().size();
        int botsToAdd = Math.min(Constants.MAX_PLAYERS - currentPlayerCount, availableColors.size());

        for (int i = 0; i < botsToAdd; i++) {
            String botColor = availableColors.get(i);
            String botName = "Bot-" + botColor;

            BotPlayer bot = new BotPlayer(botName, botColor);
            gameManager.addPlayer(bot);
        }
    }

    private void checkAndPlayBotTurn() {
        Player currentPlayer = gameManager.getCurrentPlayer();

        if (currentPlayer instanceof BotPlayer) {
            LOGGER.info("Current player is a bot - preparing to handle bot turn");

            Thread botThread = new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    playBotTurn((BotPlayer) currentPlayer);
                } catch (InterruptedException e) {
                    LOGGER.warning("Bot turn interrupted: " + e.getMessage());
                } catch (Exception e) {
                    LOGGER.severe("Unexpected error in bot turn: " + e.getMessage());
                    e.printStackTrace();

                    gameManager.nextTurn();

                    TurnChangeEvent turnEvent = new TurnChangeEvent(
                        gameManager.getCurrentPlayer().getName()
                    );
                    networkHandler.broadcast(turnEvent);

                    checkAndPlayBotTurn();
                }
            }, "BotTurn-" + currentPlayer.getColor());

            botThread.setDaemon(true);
            botThread.start();

        }
    }

    private void playBotTurn(BotPlayer bot) {
        LOGGER.info("Bot " + bot.getName() + " taking turn");

        LOGGER.info("Bot state before rolling: " +
            "Player color=" + bot.getColor() +
            ", Pawns=" + getDetailedPawnPositions(bot));

        try {
            int diceRoll = gameManager.rollDice();
            LOGGER.info("Bot rolled: " + diceRoll);

            DiceRollResultEvent diceEvent = new DiceRollResultEvent(
                diceRoll,
                bot.getColor()
            );
            networkHandler.broadcast(diceEvent);

            int[] validMoves = MoveValidator.getValidMoves(bot, diceRoll, gameManager.getBoard());
            LOGGER.info("Valid moves for bot: " + Arrays.toString(validMoves));

            // If no valid moves, end turn immediately
            if (validMoves.length == 0) {
                LOGGER.info("Bot has no valid moves, ending turn");
                gameManager.nextTurn();
                TurnChangeEvent turnEvent = new TurnChangeEvent(
                    gameManager.getCurrentPlayer().getName()
                );
                networkHandler.broadcast(turnEvent);
                checkAndPlayBotTurn();
                return;
            }

            int playerIndex = gameManager.getPlayers().indexOf(bot);
            int pawnIndex = bot.chooseBestMove(gameManager.getBoard(), diceRoll, gameManager.getPlayers());

            LOGGER.info("Bot choosing move - Player index: " + playerIndex +
                ", Pawn index: " + pawnIndex +
                ", Dice roll: " + diceRoll);

            if (pawnIndex >= 0) {
                // Execute bot's chosen move
                LOGGER.info("Bot moving pawn " + pawnIndex + " with roll " + diceRoll);

                boolean moveSuccess = gameManager.movePawn(playerIndex, pawnIndex, diceRoll);
                if (moveSuccess) {
                    Pawn movedPawn = bot.getPawns().get(pawnIndex);
                    LOGGER.info("Bot move successful - New position: " + movedPawn.getPosition());

                    MoveResultEvent moveEvent = new MoveResultEvent(
                        true,
                        "Bot moved successfully",
                        pawnIndex,
                        movedPawn.getPosition()
                    );
                    networkHandler.broadcast(moveEvent);

                    broadcastGameState();

                    if (gameManager.hasPlayerWon(bot.getName())) {
                        handleGameOver(bot.getName());
                        return;
                    }

                    boolean shouldGetExtraRoll = false;
                    String reason = "";

                    if (movedPawn.isFinished()) {
                        shouldGetExtraRoll = true;
                        reason = "Pawn reached finish base";
                    } else if (gameManager.wasPawnCaptured()) {
                        shouldGetExtraRoll = true;
                        reason = "Pawn captured";
                    } else if (diceRoll == 6) {
                        shouldGetExtraRoll = true;
                        reason = "Rolled a 6";
                    }

                    if (shouldGetExtraRoll) {
                        LOGGER.info("Bot gets another roll: " + reason);
                        gameManager.setLastDiceRoll(0); // Allow another roll
                        try {
                            Thread.sleep(1500); // Brief pause before next bot action
                            playBotTurn(bot); // Recursive call for the extra turn
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            LOGGER.warning("Bot turn interrupted: " + e.getMessage());
                        }
                    } else {
                        gameManager.nextTurn();
                        LOGGER.info("Bot turn complete, moving to next player: " +
                            gameManager.getCurrentPlayer().getName());

                        TurnChangeEvent turnEvent = new TurnChangeEvent(
                            gameManager.getCurrentPlayer().getName()
                        );
                        networkHandler.broadcast(turnEvent);

                        checkAndPlayBotTurn();
                    }
                } else {
                    LOGGER.warning("Bot move failed - Details: Player=" + playerIndex +
                        ", Pawn=" + pawnIndex +
                        ", Roll=" + diceRoll);
                }
            } else {
                gameManager.nextTurn();
                LOGGER.warning("Bot failed to choose a move, ending turn to be safe.");

                TurnChangeEvent turnEvent = new TurnChangeEvent(
                    gameManager.getCurrentPlayer().getName()
                );
                networkHandler.broadcast(turnEvent);
                checkAndPlayBotTurn();
            }
        } catch (Exception e) {
            LOGGER.severe("Error during bot turn: " + e.getMessage());
            e.printStackTrace();

            // Gracefully handle error by moving to the next turn
            gameManager.nextTurn();
            TurnChangeEvent turnEvent = new TurnChangeEvent(
                gameManager.getCurrentPlayer().getName()
            );
            networkHandler.broadcast(turnEvent);
            checkAndPlayBotTurn();
        }
    }

    private String getDetailedPawnPositions(Player player) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < player.getPawns().size(); i++) {
            Pawn pawn = player.getPawns().get(i);
            sb.append("Pawn").append(i).append("(");
            if (pawn.isHome()) {
                sb.append("home");
            } else if (pawn.isFinished()) {
                sb.append("finished");
            } else {
                sb.append("pos=").append(pawn.getPosition());
            }
            sb.append(")");
            if (i < player.getPawns().size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
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
        String playerName = connectionToPlayerMap.get(connectionId);
        return playerName != null && playerName.equals(this.adminPlayerName);
    }

    private void handleGameOver(String winner) {
        GameOverEvent gameOverEvent = new GameOverEvent(winner);
        networkHandler.broadcast(gameOverEvent);
        gameManager.setGameState(GameState.GAME_OVER);
        broadcastGameState();
    }

    private void handleLeaveRequest(LeaveGameRequestEvent event) {
        String connectionId = event.getConnection().getConnectionID();
        String playerName = connectionToPlayerMap.remove(connectionId);
        if (playerName != null) {
            Player player = gameManager.getPlayerByName(playerName);
            if (player != null) {
                player.setDisconnected(true);
                LOGGER.info("Player " + playerName + " has been marked as disconnected.");
                networkHandler.broadcast(new PlayerLeftEvent(playerName));
            }
        }
    }


    @Override
    public void onConnectionError(Exception e) {
        LOGGER.severe("Connection error: " + e.getMessage());
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

        Player disconnectedPlayer = gameManager.getPlayerByName(playerName);
        if (disconnectedPlayer != null && disconnectedPlayer.isDisconnected()) {
            LOGGER.info("Found a disconnected player with the name: " + playerName + ". Prompting user to reconnect.");
            networkHandler.sendToClient(connectionId, new ReconnectPromptEvent(playerName));
            return;
        }

        if (isNameTaken(playerName, color)) {
            networkHandler.sendToClient(connectionId, new JoinGameResponseEvent(Response.USERNAME_TAKEN));
            return;
        }

        if (isColorTaken(color)) {
            networkHandler.sendToClient(connectionId, new JoinGameResponseEvent(Response.COLOR_TAKEN));
            return;
        }

        Player newPlayer = new Player(playerName, color);
        if (gameManager.addPlayer(newPlayer)) {
            connectionToPlayerMap.put(connectionId, playerName);
            LOGGER.info("Player joined successfully: " + playerName);
            networkHandler.sendToClient(connectionId, new JoinGameResponseEvent(Response.OK));

            if (gameManager.getPlayers().size() == 1) {
                this.adminPlayerName = playerName;
                networkHandler.sendToClient(connectionId, new FirstPlayerEvent());
                LOGGER.info("First player joined and set as admin: " + playerName);
            }

            networkHandler.broadcast(new PlayerJoinedEvent(newPlayer));
            broadcastPlayerList();
        } else {
            LOGGER.warning("Join failed for player: " + playerName + " (game full)");
            networkHandler.sendToClient(connectionId, new JoinGameResponseEvent(Response.GAME_FULL));
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

    private void handleClientDisconnection(Connection connection) {
        if (connection == null) {
            LOGGER.warning("Received disconnection request with null connection");
            return;
        }

        String clientId = connection.getConnectionID();
        LOGGER.info("Processing client disconnection for: " + clientId);

        String playerName = connectionToPlayerMap.get(clientId);

        if (playerName != null && gameManager.isGameStarted()) {
            gameManager.removePlayer(playerName);
        }

        // Forward to connection handler for cleanup
        networkHandler.getServer().getClientConnectionHandler().handleClientDisconnection(connection);
    }

    public void handleSaveGameRequest(SaveGameRequestEvent event) {
        String connectionId = event.getConnection().getConnectionID();
        if (!isFirstPlayer(connectionId)) {
            LOGGER.warning("Non-admin player attempted to save game: " + connectionId);
            networkHandler.sendToClient(
                connectionId,
                new SaveGameResponseEvent(SaveGameResponseEvent.Response.NOT_AUTHORIZED)
            );
            return;
        }

        if (!gameManager.isGameStarted()) {
            LOGGER.warning("Attempted to save game before it started");
            networkHandler.sendToClient(
                connectionId,
                new SaveGameResponseEvent(SaveGameResponseEvent.Response.INVALID_STATE)
            );
            return;
        }

        try {
            String fileName = event.isAutoSave() ?
                GamePersistence.AUTO_SAVE_FILE :
                GamePersistence.SAVE_FILE;

            GamePersistence.GameSaveData saveData = new GamePersistence.GameSaveData();
            saveData.players = gameManager.getPlayers().stream()
                .map(player -> {
                    GamePersistence.PlayerSaveData playerData = new GamePersistence.PlayerSaveData();
                    playerData.name = player.getName();
                    playerData.color = player.getColor();
                    playerData.pawns = player.getPawns().stream()
                        .map(pawn -> {
                            GamePersistence.PawnSaveData pawnData = new GamePersistence.PawnSaveData();
                            pawnData.position = pawn.getPosition();
                            pawnData.isHome = pawn.isHome();
                            pawnData.isFinished = pawn.isFinished();
                            return pawnData;
                        })
                        .collect(Collectors.toList());
                    return playerData;
                })
                .collect(Collectors.toList());
            saveData.currentPlayerColor = gameManager.getCurrentPlayer().getColor();
            saveData.gameState = gameManager.getGameState();

            GamePersistence.saveGame(saveData, fileName);
            LOGGER.info("Game saved successfully to " + fileName);

            networkHandler.sendToClient(
                connectionId,
                new SaveGameResponseEvent(SaveGameResponseEvent.Response.OK, fileName)
            );
        } catch (IOException e) {
            LOGGER.severe("Failed to save game: " + e.getMessage());
            networkHandler.sendToClient(
                connectionId,
                new SaveGameResponseEvent(SaveGameResponseEvent.Response.SAVE_FAILED, null, e.getMessage())
            );
        }
    }

    public void handleLoadGameRequest(LoadGameRequestEvent event) {
        String connectionId = event.getConnection().getConnectionID();
        if (!isFirstPlayer(connectionId)) {
            LOGGER.warning("Non-admin player attempted to load game: " + connectionId);
            networkHandler.sendToClient(
                connectionId,
                new LoadGameResponseEvent(LoadGameResponseEvent.Response.NOT_AUTHORIZED)
            );
            return;
        }

        if (gameManager.isGameStarted()) {
            LOGGER.warning("Attempted to load game while one is in progress");
            networkHandler.sendToClient(
                connectionId,
                new LoadGameResponseEvent(LoadGameResponseEvent.Response.INVALID_STATE)
            );
            return;
        }

        try {
            String fileName = event.isAutoSave() ?
                GamePersistence.AUTO_SAVE_FILE :
                event.getSaveFileName();

            GamePersistence.GameSaveData saveData = GamePersistence.loadGame(fileName);
            if (saveData == null) {
                LOGGER.warning("No save file found at " + fileName);
                networkHandler.sendToClient(
                    connectionId,
                    new LoadGameResponseEvent(LoadGameResponseEvent.Response.FILE_NOT_FOUND)
                );
                return;
            }

            if (!saveData.isValid()) {
                LOGGER.warning("Invalid save data in file " + fileName);
                networkHandler.sendToClient(
                    connectionId,
                    new LoadGameResponseEvent(LoadGameResponseEvent.Response.INVALID_SAVE)
                );
                return;
            }

            loadSavedGameState(saveData, connectionId);
            LOGGER.info("Game loaded successfully from " + fileName);

            networkHandler.sendToClient(
                connectionId,
                new LoadGameResponseEvent(LoadGameResponseEvent.Response.OK, saveData)
            );
        } catch (Exception e) {
            LOGGER.severe("Failed to load game: " + e.getMessage());
            networkHandler.sendToClient(
                connectionId,
                new LoadGameResponseEvent(LoadGameResponseEvent.Response.SERVER_ERROR, null, e.getMessage())
            );
        }
    }
}
