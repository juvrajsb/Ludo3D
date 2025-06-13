package ludo.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import ludo.client.screens.LobbyScreen;
import ludo.client.screens.UsernameScreen;
import ludo.core.entities.Pawn;
import ludo.core.events.serverToClient.*;
import ludo.core.events.clientToServer.*;
import ludo.core.game.GameManager;
import ludo.core.game.GameState;
import ludo.core.network.*;
import ludo.client.networking.ClientNetworkHandler;
import ludo.client.screens.GameScreen;
import ludo.core.entities.Player;
import ludo.core.persistence.GamePersistence;

import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Logger;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

public class GameStateManager implements MessageListener {
    private static final Logger LOGGER = Logger.getLogger(GameStateManager.class.getName());

    private final LudoGame game;
    private final ClientNetworkHandler networkHandler;
    private GameManager gameManager;
    private GameScreen gameScreen;
    private GameState gameState;
    private boolean isMyTurn;
    private int currentDiceValue;
    private boolean isFirstPlayer;
    private boolean gameStarted;
    private boolean canMovePawn;
    private String currentUsername;
    private String currentColor;
    private Screen currentScreen;
    private LobbyScreen lobbyScreen;
    private UsernameScreen usernameScreen;
    private int consecutiveSixes = 0;
    private static final int MAX_CONSECUTIVE_SIXES = 3;
    private final List<Player> currentPlayers = new ArrayList<>();
    private GameState currentState;
    private volatile boolean disconnectionAcknowledged = false;

    public GameStateManager(LudoGame game) {
        this.game = game;
        this.networkHandler = new ClientNetworkHandler();
        this.networkHandler.setMessageListener(this);
        this.isFirstPlayer = false;
        this.isMyTurn = false;

        this.canMovePawn = false;
        this.gameManager = new GameManager();
        this.isFirstPlayer = false;
        this.gameStarted = false;
    }

    //for testing
    public GameStateManager() {
        this.game = null;
        this.networkHandler = new ClientNetworkHandler();
        this.networkHandler.setMessageListener(this);
        this.isFirstPlayer = false;
        this.gameStarted = false;
        this.canMovePawn = false;
    }

    //for testing
    public GameStateManager(ClientNetworkHandler networkHandler) {
        this.game = null;
        this.networkHandler = networkHandler;
        this.networkHandler.setMessageListener(this);
        this.isFirstPlayer = false;
        this.isMyTurn = false;
        this.gameStarted = false;
        this.canMovePawn = false;
    }

    public boolean connect(String ip, int port) {
//        LOGGER.info("Attempting to connect to " + ip + ":" + port);
        try {
            networkHandler.connect(ip, port);
            boolean connected = networkHandler.isConnected();
            if (connected) {
//                LOGGER.info("Successfully connected to server");
            } else {
                LOGGER.warning("Connection failed - network handler reports not connected");
            }
            return connected;
        } catch (Exception e) {
            LOGGER.severe("Connection failed: " + e.getMessage());
            return false;
        }
    }

    public boolean joinGame(String username, String color) {
        if (!networkHandler.isConnected()) {
            LOGGER.warning("Cannot join game - not connected to server");
            return false;
        }

        LOGGER.info("Attempting to join game - Username: " + username + ", Color: " + color);
        this.currentUsername = username;
        this.currentColor = color;

        JoinGameRequestEvent joinRequest = new JoinGameRequestEvent(username, color);
        try {
            networkHandler.sendMessage(joinRequest);
//            LOGGER.info("Join request sent successfully");
            return true;
        } catch (Exception e) {
            LOGGER.severe("Failed to send join request: " + e.getMessage());
            return false;
        }
    }

//    public void checkFirstPlayer(MessageListener callback) { //TODO check usage not used currently
//        if (isFirstPlayer) {
//            callback.onMessageReceived(new FirstPlayerEvent());
//        }
//    }

    public boolean isFirstPlayer() {
        return isFirstPlayer;
    }

    public void startGame(boolean enableBots, boolean loadSavedGame) {
        if (isFirstPlayer && !gameStarted) {
//            LOGGER.info("First player requesting game start with bots: " + enableBots +
//                ", load saved game: " + loadSavedGame);

            String saveFileName = null;
            if (loadSavedGame) {
                // Get the most recent save file name
                List<String> saveFiles = GamePersistence.listSaveFiles();
                if (saveFiles.isEmpty()) {
                    LOGGER.warning("No save files found");
                    return;
                }
                saveFileName = saveFiles.get(0); // Most recent save file
            }

            networkHandler.sendMessage(new StartGameRequestEvent(enableBots, loadSavedGame, saveFileName));
        } else {
            LOGGER.warning("Invalid game start request - isFirstPlayer: " +
                isFirstPlayer + ", gameStarted: " + gameStarted);
        }
    }

    public void startGame(boolean enableBots) {
        startGame(enableBots, false);
    }

//    public void startGame() {
//        startGame(false);
//    }

    /**
     * Checks if there's a saved game with matching player names
     */
    public boolean hasSavedGameWithMatchingPlayers() {
        if (!GamePersistence.hasSaveGame()) {
            return false;
        }

        GamePersistence.GameSaveData saveData = GamePersistence.loadGame();
        if (saveData == null) {
            return false;
        }

        return checkIfSavedGameMatchesCurrentPlayers(saveData);
    }

    private boolean checkIfSavedGameMatchesCurrentPlayers(GamePersistence.GameSaveData saveData) {
        if (saveData.players.size() != currentPlayers.size()) {
            return false;
        }

        // Create sets of player colors for comparison
        Set<String> currentPlayerColors = currentPlayers.stream()
            .map(Player::getColor)
            .collect(Collectors.toSet());

        Set<String> savedPlayerColors = saveData.players.stream()
            .map(playerData -> playerData.color)
            .collect(Collectors.toSet());

        return currentPlayerColors.equals(savedPlayerColors);
    }

    public void leaveGame() {
        networkHandler.sendMessage(new LeaveGameRequestEvent());
        networkHandler.disconnect();
        isFirstPlayer = false;
        gameStarted = false;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void initialize(Screen screen) {
        this.currentScreen = screen;
        if (screen instanceof GameScreen) {
            this.gameScreen = (GameScreen) screen;
        }
    }

    @Override
    public void onMessageReceived(NetworkMessage message) {
        try {
            switch (message.getType()) {
                case "FIRST_PLAYER":
                    LOGGER.info("Received first player designation");
                    handleFirstPlayer();
                    break;
                case "JOIN_GAME_RESPONSE":
                    JoinGameResponseEvent joinResponse = (JoinGameResponseEvent) message;
//                    LOGGER.info("Join response received: " + joinResponse.getResponse());
                    handleJoinResponse(joinResponse);
                    break;
                case "DICE_ROLL_RESULT":
                    handleDiceRoll(message);
                    break;
                case "MOVE_RESULT":
                    handleMoveResult(message);
                    break;
                case "GAME_STATE_UPDATE":
                    handleGameStateUpdate(message);
                    logGameState((GameStateUpdateEvent) message);
                    break;
                case "PLAYER_JOINED":
                    handlePlayerJoined(message);
                    break;
                case "TURN_CHANGE":
                    handleTurnChange(message);
                    break;
                case "GAME_OVER":
                    handleGameOver(message);
                    break;
                case "GAME_STARTED":
                    handleGameStarted((GameStartedEvent) message);
                    break;
                case "START_GAME_RESPONSE":
                    handleStartGameResponse((StartGameResponseEvent) message);
                    break;
                case "WAITING_ROOM_UPDATE":
                    handleWaitingRoomUpdate((WaitingRoomUpdateEvent) message);
                    break;
                case "ERROR":
                    ErrorEvent errorEvent = (ErrorEvent) message;
                    LOGGER.warning("Error from server: " + errorEvent.getMessage());
                    gameScreen.showMessage("Error: " + errorEvent.getMessage());
                    gameScreen.enableControls();
                    break;
                case "CAN_ROLL_AGAIN":
                    LOGGER.info("Player can roll again");
                    if (isMyTurn) {
                        gameScreen.enableControls();
                        gameScreen.showMessage("You rolled a 6! Roll again.");
                    }
                    break;
                case "DISCONNECTION_ACKNOWLEDGED":
                    handleDisconnectionAcknowledged();
                    break;
                case "SAVE_GAME_RESPONSE":
                    handleSaveGameResponse((SaveGameResponseEvent) message);
                    break;
                case "LOAD_GAME_RESPONSE":
                    handleLoadGameResponse((LoadGameResponseEvent) message);
                    break;
                default:
                    LOGGER.warning("Unhandled message type: " + message.getType());
            }
        } catch (Exception e) {
            LOGGER.severe("Error processing message " + message.getType() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void requestDiceRoll() {
        if (!isMyTurn) {
            LOGGER.warning("Attempted to roll dice when not player's turn");
            return;
        }

        LOGGER.info("Requesting dice roll");
        networkHandler.sendMessage(new DiceRollRequestEvent());
        gameScreen.disableControls();
    }

    public void requestMove(int pawnIndex) {
        if (!isMyTurn || currentDiceValue == 0) {
            LOGGER.warning("Invalid move request - Not turn or no dice roll");
            gameScreen.showMessage("Not your turn or no dice roll");
            return;
        }

//        LOGGER.info("Requesting move - Pawn: " + pawnIndex + ", Steps: " + currentDiceValue);

        gameScreen.highlightPawnAsMoving(pawnIndex);

        networkHandler.sendMessage(new MoveRequestEvent(pawnIndex, currentDiceValue));

        gameScreen.disableControls();
    }

    /**
     * This method handles the move result response from the server with improved
     * error handling to maintain proper game state after failed moves.
     */
    public void handleMoveResult(NetworkMessage message) {
        MoveResultEvent event = (MoveResultEvent) message;

        LOGGER.info("Handling move result - Success: " + event.isSuccess() +
            ", Message: " + event.getMessage() +
            ", Current Game State: " + gameState +
            ", Is My Turn: " + isMyTurn +
            ", Current Dice Value: " + currentDiceValue);

        if (event.isSuccess()) {
            gameScreen.playMoveAnimation(event.getPawnIndex(), event.getNewPosition());
            gameScreen.disableControls();

            // Reset currentDiceValue after successful move
            currentDiceValue = 0;

            // Don't change game state here - wait for server update
        } else {
            LOGGER.warning("Move failed: " + event.getMessage());
            gameScreen.showMessage(event.getMessage());

            if (isMyTurn) {
                LOGGER.info("Move failed - disabling controls");
                gameScreen.disableControls();
                canMovePawn = false;

                // Only re-enable if we're still in a valid state for moving and haven't used our roll
                if (gameState == GameState.WAITING_FOR_MOVE && currentDiceValue > 0) {
                    gameScreen.enablePawnSelection();
                    canMovePawn = true;
                }
            }
        }
    }

    public void setGameState(GameState state) {
        this.gameState = state;
        if (gameScreen != null) {
            gameScreen.updateGameState(state);
        }

        LOGGER.info("Game state set to: " + state);
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public String getCurrentColor() {
        return currentColor;
    }

    private void logGameState(GameStateUpdateEvent event) {
        StringBuilder state = new StringBuilder("\nCurrent Game State:\n");
        state.append("Current Player: ").append(event.getCurrentPlayer()).append("\n");
        state.append("Game State: ").append(event.getGameState()).append("\n");
        state.append("Pawn Positions:\n");
        event.getPawnPositions().forEach((color, positions) -> {
            state.append("  ").append(color).append(": ").append(positions).append("\n");
        });
        LOGGER.info(state.toString());
    }

    private void handleFirstPlayer() {
//        LOGGER.info("handleFirstPlayer: Setting first player flag from first player event");
        isFirstPlayer = true;
        if (lobbyScreen != null) {
//            LOGGER.info("handleFirstPlayer: Calling lobbyScreen.setFirstPlayer()");
            lobbyScreen.setFirstPlayer();
        } else {
            LOGGER.warning("handleFirstPlayer: lobbyScreen is null");
        }
    }

    public void handleWaitingRoomUpdate(WaitingRoomUpdateEvent event) {
        LOGGER.info("Updating lobby with " + event.getUsernames().size() + " players");

        currentPlayers.clear();
        for (String username : event.getUsernames()) {
            currentPlayers.add(new Player(username, event.getColorForPlayer(username)));
        }

        if (lobbyScreen != null) {
            lobbyScreen.updatePlayersList(currentPlayers);
        }
    }

    public void setCurrentScreen(Screen screen) {
        this.currentScreen = screen;
        if (screen instanceof GameScreen) {
            this.gameScreen = (GameScreen) screen;
        } else if (screen instanceof UsernameScreen) {
            this.usernameScreen = (UsernameScreen) screen;
        }
    }

    @Override
    public void onConnectionError(Exception error) {
        LOGGER.severe("Connection error: " + error.getMessage());
        if (gameScreen != null) {
            gameScreen.showMessage("Connection error: " + error.getMessage());
        }
    }

    public void setLobbyScreen(LobbyScreen screen) {
        if (this.lobbyScreen == null) {
            this.lobbyScreen = screen;
            if (isFirstPlayer && screen != null) {
//                LOGGER.info("Setting first player status on new lobby screen");
                screen.setFirstPlayer();
            }
        }
    }

    private void handleJoinResponse(JoinGameResponseEvent event) {
//        LOGGER.info("Processing join response: " + event.getResponse());

        if (event.getResponse() == Response.FIRST_PLAYER || event.getResponse() == Response.OK) {
            if (event.getResponse() == Response.FIRST_PLAYER) {
                isFirstPlayer = true;
                LOGGER.info("This client is the first player");
            } else {
                isFirstPlayer = false;
//                LOGGER.info("This client is not the first player");
            }

            Gdx.app.postRunnable(() -> {
                if (currentScreen instanceof UsernameScreen) {
                    UsernameScreen screen = (UsernameScreen) currentScreen;
                    screen.onJoinResponse(event.getResponse());
                } else {
                    LOGGER.warning("Not on username screen when join response received");
                    game.setScreen(new LobbyScreen(game));
                }
            });
        } else {
            if (currentScreen instanceof UsernameScreen) {
                Gdx.app.postRunnable(() -> {
                    UsernameScreen screen = (UsernameScreen) currentScreen;
                    screen.onJoinResponse(event.getResponse());
                });
            }
        }
    }

    public void handleGameStarted(GameStartedEvent event) {
//        LOGGER.info("Game started event received");
        gameStarted = true;

        // Initialize game state
        LOGGER.info("Current players in GameStateManager: " + currentPlayers.size());
        LOGGER.info("Players received in event: " + event.getPlayers().size());

        // Store event data for main thread processing
        final List<Player> players = new ArrayList<>(event.getPlayers());
        final String startingPlayerName = event.getStartingPlayer();

        // Create game screen on the main thread
        Gdx.app.postRunnable(() -> {
            try {
                // Create game screen first to ensure it exists before any events
                GameScreen newGameScreen = new GameScreen(game);
                this.gameScreen = newGameScreen;
                this.currentScreen = newGameScreen;

                // Clear and update currentPlayers list
                currentPlayers.clear();
                players.forEach(player -> {
//                    LOGGER.info("Adding player to game: " + player.getName());
                    newGameScreen.addPlayer(player);
                    currentPlayers.add(player);
                });

                newGameScreen.setCurrentPlayer(startingPlayerName);
                isMyTurn = currentUsername.equals(startingPlayerName);

                // Set up initial game state
                if (isMyTurn) {
                    newGameScreen.enableControls();
                    newGameScreen.showMessage("Your turn!");
                } else {
                    newGameScreen.disableControls();
                    newGameScreen.showMessage("Waiting for " + startingPlayerName);
                }

                // Switch to game screen after all initialization is complete
                game.setScreen(newGameScreen);
//                LOGGER.info("Screen transition complete. Current player: " + currentUsername +
//                    ", isMyTurn: " + isMyTurn);
            } catch (Exception e) {
                LOGGER.severe("Error creating game screen: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public List<Player> getCurrentPlayers() {
        return new ArrayList<>(currentPlayers);
    }

    public void handleStartGameResponse(StartGameResponseEvent event) {
//        LOGGER.info("Start game response received: " + event.getResponse());

        if (event.isSuccess()) {
            LOGGER.info("Game successfully started");
            gameStarted = true;
        } else {
            LOGGER.warning("Failed to start game: " + event.getMessage());
            if (lobbyScreen != null) {
                lobbyScreen.showError("Failed to start game: " + event.getMessage());
            }
        }
    }
//todo centralize this method with ServerGameStateManager
    public void handleDiceRoll(NetworkMessage message) {
        DiceRollResultEvent event = (DiceRollResultEvent) message;
        currentDiceValue = event.getValue();
//        LOGGER.info("Dice roll result received: " + currentDiceValue);

        if (gameScreen == null) {
            LOGGER.warning("GameScreen is null when handling dice roll result");
            return;
        }

        gameScreen.updateDiceDisplay(currentDiceValue);

        if (isMyTurn) {
            if (currentDiceValue == 6) {
                consecutiveSixes++;
                LOGGER.info("Consecutive sixes: " + consecutiveSixes);

                if (consecutiveSixes >= MAX_CONSECUTIVE_SIXES) {
                    LOGGER.info("Maximum consecutive sixes reached - ending turn");
                    gameScreen.showMessage("Three sixes in a row - turn forfeited!");
                    networkHandler.sendMessage(new TurnEndEvent());
                    isMyTurn = false;
                    consecutiveSixes = 0;
                    return;
                }
            } else {
                consecutiveSixes = 0;
            }

            Player currentPlayer = gameScreen.getCurrentPlayer();
//            LOGGER.info("Processing move options for player: " + currentPlayer.getName());

            if (currentPlayer == null) {
                LOGGER.severe("Current player is null during dice roll handling");
                return;
            }

            // Log detailed pawn state
//            LOGGER.info("Current player pawns state:");
            for (Pawn pawn : currentPlayer.getPawns()) {
                LOGGER.info("Pawn - Position: " + pawn.getPosition() +
                    ", isHome: " + pawn.isHome() +
                    ", isFinished: " + pawn.isFinished());
            }

            boolean hasValidMove = hasValidMovesAvailable(currentPlayer, currentDiceValue);
//            LOGGER.info("Move validation result: " + hasValidMove);

            if (hasValidMove) {
//                LOGGER.info("=== Enabling Move Selection ===");
//                LOGGER.info("Before enable - canMovePawn: " + canMovePawn);
                gameScreen.enablePawnSelection();
                canMovePawn = true;
//                LOGGER.info("After enable - canMovePawn: " + canMovePawn);

                if (currentDiceValue == 6) {
                    gameScreen.showMessage("Roll again after moving a pawn");
                } else {
                    gameScreen.showMessage("Select a pawn to move");
                }
            } else {
                LOGGER.info("No valid moves available - automatically ending turn");
                gameScreen.showMessage("No valid moves - turn skipped");
                gameScreen.disableControls();
                networkHandler.sendMessage(new TurnEndEvent());
                isMyTurn = false;
            }
        }
    }

    private boolean hasValidMovesAvailable(Player player, int roll) {
        LOGGER.info("Validating moves for player " + player.getName() + " with roll " + roll);

        boolean hasValidMove = false;
        for (int i = 0; i < player.getPawns().size(); i++) {
            Pawn pawn = player.getPawns().get(i);
            LOGGER.info("Checking pawn " + i + " - Position: " + pawn.getPosition() +
                ", isHome: " + pawn.isHome() +
                ", isFinished: " + pawn.isFinished());

            // If pawn is home, it can only move with a 6
            if (pawn.isHome()) {
                if (roll == 6) {
//                    LOGGER.info("Found valid move - pawn " + i + " can leave home with roll 6");
                    hasValidMove = true;
                }
                continue;
            }

            // If pawn is finished, it can't move
            if (pawn.isFinished()) {
                LOGGER.info("Pawn " + i + " is finished, skipping");
                continue;
            }

            // For pawns outside home, any roll is valid
            LOGGER.info("Found valid move for pawn " + i + " from position " +
                pawn.getPosition() + " with roll " + roll);
            hasValidMove = true;
        }

        LOGGER.info("Move validation complete - Has valid moves: " + hasValidMove);
        return hasValidMove;
    }

    public void setCurrentPlayer(String color) {
        if (gameScreen != null) {
            LOGGER.info("Setting current player to: " + color +
                ", Current Game State: " + gameState +
                ", Is My Turn: " + isMyTurn +
                ", Current Dice Value: " + currentDiceValue);

            gameScreen.setCurrentPlayer(color);
            isMyTurn = color.equals(currentColor);
            if (isMyTurn) {
                gameScreen.enableControls();
                gameScreen.showMessage("Your turn!");
            } else {
                gameScreen.disableControls();
                gameScreen.showMessage("Waiting for " + color);
            }
        }
    }

    private void handleGameStateUpdate(NetworkMessage message) {
        GameStateUpdateEvent event = (GameStateUpdateEvent) message;

        if (gameScreen != null) {
            event.getPawnPositions().forEach((color, positions) ->
                gameScreen.updatePlayerPawns(color, positions)
            );

            gameScreen.setCurrentPlayer(event.getCurrentPlayer());

            // Update game state based on server's state
            setGameState(event.getGameState());

            // Only enable controls if it's our turn and we're in a valid state
            if (isMyTurn && (event.getGameState() == GameState.IN_PROGRESS ||
                           event.getGameState() == GameState.WAITING_FOR_MOVE)) {
                gameScreen.enableControls();
                if (currentDiceValue == 0) {
                    gameScreen.enableRollButton();
                } else {
                    gameScreen.enablePawnSelection();
                }
            } else {
                gameScreen.disableControls();
            }
        }
    }

//    private void updateUIForGameState(GameState state, String currentPlayerColor) {
//        isMyTurn = currentPlayerColor.equals(currentColor);
//
//        if (isMyTurn) {
//            switch (state) {
//                case IN_PROGRESS:
//                    gameScreen.enableControls();
//                    gameScreen.showMessage("Your turn! Roll the dice");
//                    break;
//
//                case DICE_ROLLED:
//                    gameScreen.disableControls();
//                    gameScreen.showMessage("Dice rolled. Wait for server...");
//                    break;
//
//                case WAITING_FOR_MOVE:
//                    gameScreen.enablePawnSelection();
//                    gameScreen.showMessage("Select a pawn to move");
//                    break;
//
//                default:
//                    gameScreen.disableControls();
//                    gameScreen.showMessage("Waiting for server...");
//                    break;
//            }
//        } else {
//            gameScreen.disableControls();
//            gameScreen.showMessage("Waiting for " + currentPlayerColor);
//        }
//    }

    private void handlePlayerJoined(NetworkMessage message) {
        PlayerJoinedEvent event = (PlayerJoinedEvent) message;
        Player newPlayer = event.getPlayer();

        if (gameScreen != null) {
            gameScreen.addPlayer(newPlayer);
        } else if (lobbyScreen != null) {
            lobbyScreen.addPlayer(newPlayer);
        }
    }

    private void handleTurnChange(NetworkMessage message) {
        TurnChangeEvent event = (TurnChangeEvent) message;
        String currentPlayerName = event.getCurrentPlayer();

        // Find the player object to get their color
        Player currentPlayer = currentPlayers.stream()
            .filter(p -> p.getName().equals(currentPlayerName))
            .findFirst()
            .orElse(null);

        if (currentPlayer == null) {
            LOGGER.severe("Current player not found in player list: " + currentPlayerName);
            return;
        }

        isMyTurn = currentPlayerName.equals(currentUsername);
//        LOGGER.info("Turn changed to: " + currentPlayerName + " (Color: " + currentPlayer.getColor() +
//            ", isMyTurn: " + isMyTurn + ")");
        currentDiceValue = 0;
        consecutiveSixes = 0;

        if (gameScreen == null) {
            LOGGER.warning("GameScreen is null when handling turn change");
            return;
        }

        gameScreen.disableControls();
        gameScreen.setCurrentPlayer(currentPlayer.getColor());

        if (isMyTurn) {
            gameScreen.enableControls();
            gameScreen.showMessage("Your turn! Roll the dice");
        } else {
            gameScreen.disableControls();
            gameScreen.showMessage("Waiting for " + currentPlayerName);
        }

        // Log the current state for debugging
//        LOGGER.info("Turn change complete - Current player: " + currentPlayerName +
//            ", Color: " + currentPlayer.getColor() +
//            ", isMyTurn: " + isMyTurn);
    }

    public void handleGameOver(NetworkMessage message) {
        GameOverEvent event = (GameOverEvent) message;
        gameScreen.showWinnerScreen(event.getWinner());
    }

    public void dispose() {
        LOGGER.info("Disposing GameStateManager");
        if (networkHandler != null) {
            networkHandler.disconnect();
        }
    }

    public void resetGame() {
        LOGGER.info("Resetting game state");
        gameStarted = false;
        isMyTurn = false;
        currentDiceValue = 0;
        consecutiveSixes = 0;
        canMovePawn = false;
        currentPlayers.clear();
        if (gameScreen != null) {
            gameScreen.reset();
        }
    }

//    public void handlePlayerLeft(String playerName) { //TODO check usage not used currently
//        gameScreen.removePlayer(playerName);
//    }
//
//    public GameScreen getGameScreen() { //TODO check usage not used currently
//        return gameScreen;
//    }

    public boolean isConnected() {
        return networkHandler.isConnected();
    }

    public ClientNetworkHandler getNetworkHandler() {
        return networkHandler;
    }

    public void sendMessage(NetworkMessage message) {
        networkHandler.sendMessage(message);
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(GameState newState) {
        this.currentState = newState;
    }

    private void handleDisconnectionAcknowledged() {
        LOGGER.info("Server acknowledged disconnection");
        disconnectionAcknowledged = true;
    }

    public boolean isDisconnectionAcknowledged() {
        return disconnectionAcknowledged;
    }

    public void resetDisconnectionState() {
        disconnectionAcknowledged = false;
    }

    private void handleSaveGameResponse(SaveGameResponseEvent event) {
        switch (event.getResponse()) {
            case OK:
                LOGGER.info("Game saved successfully to " + event.getSaveFileName());
                if (gameScreen != null) {
                    gameScreen.showMessage("Game saved successfully");
                }
                break;
            case NOT_AUTHORIZED:
                LOGGER.warning("Not authorized to save game");
                if (gameScreen != null) {
                    gameScreen.showMessage("Only the first player can save the game");
                }
                break;
            case INVALID_STATE:
                LOGGER.warning("Invalid game state for saving");
                if (gameScreen != null) {
                    gameScreen.showMessage("Cannot save game in current state");
                }
                break;
            case SAVE_FAILED:
            case SERVER_ERROR:
                LOGGER.severe("Failed to save game: " + event.getErrorMessage());
                if (gameScreen != null) {
                    gameScreen.showMessage("Failed to save game: " + event.getErrorMessage());
                }
                // Attempt to recover from save failure
                handleSaveFailure();
                break;
        }
    }

    private void handleLoadGameResponse(LoadGameResponseEvent event) {
        switch (event.getResponse()) {
            case OK:
                LOGGER.info("Game loaded successfully");
                if (event.getSaveData() != null) {
                    // Clear current game state
                    game.reset();

                    // Load players and pawns
                    event.getSaveData().players.forEach(playerData -> {
                        Player player = new Player(playerData.name, playerData.color);
                        for (int i = 0; i < playerData.pawns.size(); i++) {
                            GamePersistence.PawnSaveData pawnData = playerData.pawns.get(i);
                            Pawn pawn = player.getPawns().get(i);
                            pawn.setPosition(pawnData.position);
                            if (pawnData.isHome) pawn.sendHome();
                            if (pawnData.isFinished) pawn.setFinished(true);
                        }
                        game.addPlayer(player);
                    });

                    // Set current player
                    game.getGameStateManager().setCurrentPlayer(event.getSaveData().currentPlayerColor);

                    // Switch to game screen
                    game.setScreen(new GameScreen(game));
                }
                break;
            case NOT_AUTHORIZED:
                LOGGER.warning("Not authorized to load game");
                if (gameScreen != null) {
                    gameScreen.showMessage("Only the first player can load the game");
                }
                break;
            case FILE_NOT_FOUND:
                LOGGER.warning("Save file not found");
                if (gameScreen != null) {
                    gameScreen.showMessage("No save file found");
                }
                break;
            case INVALID_SAVE:
                LOGGER.warning("Invalid save data");
                if (gameScreen != null) {
                    gameScreen.showMessage("Invalid save data");
                }
                break;
            case INVALID_STATE:
                LOGGER.warning("Invalid game state for loading");
                if (gameScreen != null) {
                    gameScreen.showMessage("Cannot load game in current state");
                }
                break;
            case SERVER_ERROR:
                LOGGER.severe("Failed to load game: " + event.getErrorMessage());
                if (gameScreen != null) {
                    gameScreen.showMessage("Failed to load game: " + event.getErrorMessage());
                }
                // Attempt to recover from load failure
                handleLoadFailure();
                break;
        }
    }

    private void handleSaveFailure() {
        LOGGER.info("Attempting to recover from save failure");

        // If we're in a game, try to auto-save
        if (gameStarted) {
            LOGGER.info("Attempting auto-save as recovery");
            requestSaveGame(true);
        }
    }

    private void handleLoadFailure() {
        LOGGER.info("Attempting to recover from load failure");

        // If we're the first player and not in a game, try to start a new game
        if (isFirstPlayer && !gameStarted) {
            LOGGER.info("Attempting to start new game as recovery");
            startGame(false);
        }
    }

    public void requestSaveGame(boolean isAutoSave) {
        if (isFirstPlayer && gameStarted) {
            LOGGER.info("Requesting game save (auto-save: " + isAutoSave + ")");
            networkHandler.sendMessage(new SaveGameRequestEvent(isAutoSave));
        } else {
            LOGGER.warning("Invalid save request - isFirstPlayer: " + isFirstPlayer +
                ", gameStarted: " + gameStarted);
        }
    }

    public void requestLoadGame(String saveFileName, boolean isAutoSave) {
        if (isFirstPlayer && !gameStarted) {
            LOGGER.info("Requesting game load - File: " + saveFileName +
                ", Auto-save: " + isAutoSave);
            networkHandler.sendMessage(new LoadGameRequestEvent(saveFileName, isAutoSave));
        } else {
            LOGGER.warning("Invalid load request - isFirstPlayer: " + isFirstPlayer +
                ", gameStarted: " + gameStarted);
        }
    }
}
