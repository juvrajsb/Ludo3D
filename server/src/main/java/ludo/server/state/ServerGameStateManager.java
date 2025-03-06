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

import java.util.*;
import java.util.stream.Collectors;

import static ludo.server.Server.LOGGER;

public class ServerGameStateManager implements MessageListener {
    private final GameManager gameManager;
    private final ServerNetworkHandler networkHandler;
    private final Map<String, String> connectionToPlayerMap;
    private static final String[] COLOR_ORDER = {"YELLOW", "BLUE", "RED", "GREEN"};

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
        LOGGER.info(String.format("Player %s rolled %d", playerName, diceValue));

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
            }, 1500); // 1.5 second delay
        } else {
            gameManager.setGameState(GameState.WAITING_FOR_MOVE);
            broadcastGameState();
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

        LOGGER.info(String.format("Move request from %s (connection: %s)", playerName, connectionId));

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
        LOGGER.info("Positions before move: " + gameManager.getCurrentPawnPositions());

        Pawn selectedPawn = currentPlayer.getPawns().get(pawnIndex);
        boolean isLeavingHome = selectedPawn.isHome() && steps == 6;
        int currentPosition = selectedPawn.isHome() ? -1 : selectedPawn.getPosition();

        boolean moveSuccess = gameManager.movePawn(playerIndex, pawnIndex, steps);
        LOGGER.info("Move result: " + (moveSuccess ? "Success" : "Failed"));

        if (moveSuccess) {
            LOGGER.info("Positions after move: " + gameManager.getCurrentPawnPositions());
            int newPosition = currentPlayer.getPawns().get(pawnIndex).getPosition();

            LOGGER.info(String.format("Pawn moved from %d to %d (isLeavingHome: %b)",
                currentPosition, newPosition, isLeavingHome));

            MoveResultEvent resultEvent = new MoveResultEvent(
                true, "Move successful", pawnIndex, newPosition);
            networkHandler.broadcast(resultEvent);

            gameManager.setGameState(GameState.PLAYER_MOVED);
            broadcastGameState();

            if (gameManager.hasPlayerWon(playerName)) {
                handleGameOver(playerName);
                return;
            }

            if (steps == 6) {
                LOGGER.info("Player rolled a 6 - gets another turn");
                gameManager.setGameState(GameState.IN_PROGRESS);

//                boolean isBot = currentPlayer instanceof BotPlayer;

//                if (!isBot) {
                    gameManager.setLastDiceRoll(0);
//                }
                networkHandler.sendToClient(connectionId, new ludo.core.events.serverToClient.CanRollAgainEvent());
            } else {
                gameManager.nextTurn();
                String nextPlayer = gameManager.getCurrentPlayer().getName();
                LOGGER.info("Next player: " + nextPlayer);
                networkHandler.broadcast(new ludo.core.events.serverToClient.TurnChangeEvent(nextPlayer));
                checkAndPlayBotTurn();
            }

            broadcastGameState();
        } else {
            LOGGER.warning("Move failed for pawn " + pawnIndex);

            String failureReason = getFailureReason(selectedPawn, steps);
            sendMoveResponse(connectionId, false, failureReason, pawnIndex, -1);

            gameManager.setGameState(GameState.WAITING_FOR_MOVE);

            broadcastGameState();
        }
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

//    private void handleTurnLogicAfterMove(int steps, boolean isLeavingHome, Player currentPlayer) {
//        if (steps == 6) {
//            // If player rolled a 6, they get another turn
//            if (isLeavingHome) {
//                LOGGER.info("Pawn left home with a 6 - player gets another roll");
//                // Reset last dice roll to 0 to allow another roll
//                gameManager.setLastDiceRoll(0);
//
//                // Set game state to allow another roll
//                gameManager.setGameState(GameState.IN_PROGRESS);
//            } else {
//                LOGGER.info("Player moved with a 6 - player gets another roll");
//                // Reset last dice roll to 0 to allow another roll
//                gameManager.setLastDiceRoll(0);
//
//                // Set game state to allow another roll
//                gameManager.setGameState(GameState.IN_PROGRESS);
//            }
//
//            // Notify client they can roll again
//            networkHandler.sendToClient(
//                gameManager.getCurrentPlayer().getName(),
//                new CanRollAgainEvent()
//            );
//        } else {
//            // Move to next player's turn
//            gameManager.nextTurn();
//            String nextPlayer = gameManager.getCurrentPlayer().getName();
//            LOGGER.info("Next player: " + nextPlayer);
//
//            // Send turn change event
//            TurnChangeEvent turnEvent = new TurnChangeEvent(nextPlayer);
//            networkHandler.broadcast(turnEvent);
//        }
//    }

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
                LOGGER.info("Adding player to game: " + player.getName() + " with color: " + player.getColor());
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

        if (gameManager.isGameStarted()) {
            networkHandler.sendToClient(
                connectionId,
                new StartGameResponseEvent(StartGameResponseEvent.Response.GAME_ALREADY_STARTED)
            );
            return;
        }

        // Check if we should load a saved game
        boolean loadSavedGame = event.isLoadSavedGame();
        if (loadSavedGame && GamePersistence.hasSaveGame()) {
            GamePersistence.GameSaveData saveData = GamePersistence.loadGame();
            if (saveData != null) {
                LOGGER.info("Loading saved game");
                loadSavedGameState(saveData);

                networkHandler.sendToClient(
                    connectionId,
                    new StartGameResponseEvent(StartGameResponseEvent.Response.OK)
                );

                broadcastGameState();
                checkAndPlayBotTurn();
                return;
            }
        }

        LOGGER.info("Starting new game with " + realPlayerCount + " real players" +
            (enableBots ? " and bot players" : ""));

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

    private void loadSavedGameState(GamePersistence.GameSaveData saveData) {
        gameManager.clearPlayers();

        for (GamePersistence.PlayerSaveData playerData : saveData.players) {
            Player player = new Player(playerData.name, playerData.color);
            gameManager.addPlayer(player);

            for (int i = 0; i < playerData.pawns.size(); i++) {
                GamePersistence.PawnSaveData pawnData = playerData.pawns.get(i);
                Pawn pawn = player.getPawns().get(i);
                pawn.setPosition(pawnData.position);
                if (pawnData.isHome) pawn.sendHome();
                if (pawnData.isFinished) pawn.setFinished(true);
            }
        }

        gameManager.setGameState(saveData.gameState);
        gameManager.startGame();

        String currentPlayerColor = saveData.currentPlayerColor;
        for (Player player : gameManager.getPlayers()) {
            if (player.getColor().equalsIgnoreCase(currentPlayerColor)) {
                break;
            }
        }

        GameStartedEvent gameStartedEvent = new GameStartedEvent(
            gameManager.getPlayers(),
            saveData.currentPlayerColor,
            gameManager.getPlayers().size()
        );
        networkHandler.broadcast(gameStartedEvent);

        LOGGER.info("Saved game loaded successfully");
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

            LOGGER.info("Added bot player: " + botName + " (" + botColor + ")");
        }
    }

    private void checkAndPlayBotTurn() {
        Player currentPlayer = gameManager.getCurrentPlayer();

        LOGGER.info("Checking if current player is a bot: " +
            currentPlayer.getName() + " (" + currentPlayer.getColor() + ")");

        if (currentPlayer instanceof BotPlayer) {
            LOGGER.info("Current player is a bot - preparing to handle bot turn");

            Thread botThread = new Thread(() -> {
                try {
                    LOGGER.info("Bot will take its turn in 1.5 seconds");
                    Thread.sleep(1500);

                    LOGGER.info("Starting bot turn execution for " + currentPlayer.getName());
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

            LOGGER.info("Bot turn thread started for " + currentPlayer.getName());
        } else {
            LOGGER.info("Current player is human: " + currentPlayer.getName());
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
                } else {
                    LOGGER.warning("Bot move failed - Details: Player=" + playerIndex +
                        ", Pawn=" + pawnIndex +
                        ", Roll=" + diceRoll);
                }
            } else {
                LOGGER.info("Bot has no valid moves");
            }

            if (diceRoll == 6) {
                try {
                    LOGGER.info("Bot rolled 6, taking another turn");
                    Thread.sleep(1500); // Brief pause before next bot action
                    playBotTurn(bot);
                } catch (InterruptedException e) {
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
        } catch (Exception e) {
            LOGGER.severe("Error during bot turn: " + e.getMessage());
            e.printStackTrace();

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

        if (isNameTaken(playerName.toUpperCase(), color.toUpperCase())) {
            networkHandler.sendToClient(connectionId,
                new JoinGameResponseEvent(Response.USERNAME_TAKEN));
            return;
        }
        if (isColorTaken(color.toUpperCase())) {
            networkHandler.sendToClient(connectionId,
                new JoinGameResponseEvent(Response.COLOR_TAKEN));
            return;
        }

        Player newPlayer = new Player(playerName, color);
        if (gameManager.addPlayer(newPlayer)) {
            connectionToPlayerMap.put(connectionId, playerName);
            LOGGER.info("Player joined successfully: " + event.getPlayerName());

            networkHandler.sendToClient(connectionId,
                new JoinGameResponseEvent(Response.OK));

            if (gameManager.getPlayers().size() == 1) {
                networkHandler.sendToClient(connectionId,
                    new FirstPlayerEvent());
                LOGGER.info("First player joined: " + event.getPlayerName());
            }

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
}
