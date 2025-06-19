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
import ludo.server.handlers.*;

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
    private final Map<String, IEventHandler> eventHandlers;

    public ServerGameStateManager(ServerNetworkHandler networkHandler) {
        this.networkHandler = networkHandler;
        this.gameManager = GameManager.getInstance();
        this.connectionToPlayerMap = new HashMap<>();
        this.eventHandlers = new HashMap<>();
        initializeEventHandlers();
    }

    private void initializeEventHandlers() {
        eventHandlers.put("JOIN_GAME_REQUEST", new JoinGameRequestHandler());
        eventHandlers.put("LEAVE_GAME_REQUEST", new LeaveGameRequestHandler());
        eventHandlers.put("DICE_ROLL_REQUEST", new DiceRollRequestHandler());
        eventHandlers.put("MOVE_REQUEST", new MoveRequestHandler());
        eventHandlers.put("START_GAME_REQUEST", new StartGameRequestHandler());
        eventHandlers.put("TURN_END", new TurnEndHandler());
        eventHandlers.put("PONG", new PongHandler());
        eventHandlers.put("SAVE_GAME_REQUEST", new SaveGameRequestHandler());
        eventHandlers.put("LOAD_GAME_REQUEST", new LoadGameRequestHandler());
        eventHandlers.put("REQUEST_SAVE_FILES", new RequestSaveFilesHandler());
        eventHandlers.put("RECONNECT_REQUEST", new ReconnectRequestHandler());
    }

    @Override
    public void onMessageReceived(NetworkMessage message) {
        if (message instanceof Event event) {
            String clientId = event.getConnection() != null ? event.getConnection().getConnectionID() : null;
            if (!Objects.equals(event.getType(), "PONG")) {
                LOGGER.info("Received event: " + event.getType() + (clientId != null ? " from " + clientId : ""));
            }

            if (event instanceof ClientDisconnectedEvent) {
                handleClientDisconnection(event.getConnection());
                return;
            }

            IEventHandler handler = eventHandlers.get(event.getType());
            if (handler != null) {
                try {
                    handler.handle(event, this);
                } catch (Exception e) {
                    LOGGER.severe("Error handling event " + event.getType() + ": " + e.getMessage());
                    e.printStackTrace();
                    onConnectionError(e);
                }
            } else {
                LOGGER.warning("No handler found for event type: " + event.getType());
            }
        }
    }

    public GameManager getGameManager() { return gameManager; }
    public ServerNetworkHandler getNetworkHandler() { return networkHandler; }
    public Map<String, String> getConnectionToPlayerMap() { return connectionToPlayerMap; }
    public String getAdminPlayerName() { return adminPlayerName; }
    public void setAdminPlayerName(String name) { this.adminPlayerName = name; }

    public void handleClientDisconnection(Connection connection) {
        if (connection == null) {
            LOGGER.warning("Received disconnection request with null connection");
            return;
        }

        String clientId = connection.getConnectionID();
        LOGGER.info("Processing client disconnection for: " + clientId);

        String playerName = connectionToPlayerMap.remove(clientId);
        if (playerName != null && !gameManager.isGameStarted()) {
            gameManager.removePlayer(playerName);
        }

        networkHandler.getServer().getClientConnectionHandler().handleClientDisconnection(connection);
        broadcastPlayerList();
    }

    public void broadcastPlayerList() {
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

    public boolean isFirstPlayer(String connectionId) {
        String playerName = connectionToPlayerMap.get(connectionId);
        return playerName != null && playerName.equals(this.adminPlayerName);
    }

    @Override
    public void onConnectionError(Exception e) {
        LOGGER.severe("Connection error: " + e.getMessage());
    }

    public void broadcastGameState() {
        GameStateUpdateEvent stateEvent = new GameStateUpdateEvent(
            gameManager.getCurrentPawnPositions(),
            gameManager.getCurrentPlayer().getName(),
            gameManager.getGameState()
        );
        networkHandler.broadcast(stateEvent);
    }

    public void initializePlayersWithColors(List<Player> realPlayers, boolean enableBots) {
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

    public void loadSavedGameState(GamePersistence.GameSaveData saveData, String connectionId) {
        gameManager.clearPlayers();

        String loadingPlayerName = connectionToPlayerMap.get(connectionId);
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

    public void addBotPlayers() {
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

    public void checkAndPlayBotTurn() {
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
            broadcastGameState(); // Broadcast state change after dice roll

            int[] validMoves = MoveValidator.getValidMoves(bot, diceRoll, gameManager.getBoard());
            LOGGER.info("Valid moves for bot: " + Arrays.toString(validMoves));

            // If no valid moves, end turn immediately
            if (validMoves.length == 0) {
                LOGGER.info("Bot has no valid moves, ending turn");
                Thread.sleep(1500); // Wait for players to see the roll
                gameManager.nextTurn();
                TurnChangeEvent turnEvent = new TurnChangeEvent(
                    gameManager.getCurrentPlayer().getName()
                );
                networkHandler.broadcast(turnEvent);
                broadcastGameState();
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
                Thread.sleep(1000); // Pause before moving
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
                        checkAndPlayBotTurn(); // Recursive call for the extra turn
                    } else {
                        gameManager.nextTurn();
                        LOGGER.info("Bot turn complete, moving to next player: " +
                            gameManager.getCurrentPlayer().getName());

                        TurnChangeEvent turnEvent = new TurnChangeEvent(
                            gameManager.getCurrentPlayer().getName()
                        );
                        networkHandler.broadcast(turnEvent);
                        broadcastGameState();
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
                broadcastGameState();
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
            broadcastGameState();
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

    public void logGameState() {
        StringBuilder state = new StringBuilder("Current Game State:\n");
        state.append("Players: ").append(gameManager.getPlayers().size()).append("\n");
        for (Player player : gameManager.getPlayers()) {
            state.append("- ").append(player.getName())
                .append(" (").append(player.getColor()).append(")\n");
        }
        state.append("Current Player: ").append(gameManager.getCurrentPlayer().getName());
        LOGGER.info(state.toString());
    }

    public void handleGameOver(String winner) {
        GameOverEvent gameOverEvent = new GameOverEvent(winner);
        networkHandler.broadcast(gameOverEvent);
        gameManager.setGameState(GameState.GAME_OVER);
        broadcastGameState();
    }
}
