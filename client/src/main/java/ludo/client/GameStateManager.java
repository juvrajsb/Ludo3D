package ludo.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import ludo.client.screens.LobbyScreen;
import ludo.client.screens.UsernameScreen;
import ludo.core.entities.Pawn;
import ludo.core.events.serverToClient.*;
import ludo.core.events.clientToServer.*;
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

public class GameStateManager implements MessageListener {
    private static final Logger LOGGER = Logger.getLogger(GameStateManager.class.getName());

    private final LudoGame game;
    private final ClientNetworkHandler networkHandler;
    private GameScreen gameScreen;
    private GameState gameState;
    private boolean isMyTurn;
    private int currentDiceValue;
    private boolean isFirstPlayer;
    private boolean gameStarted;
    private String currentUsername;
    private String currentColor;
    private Screen currentScreen;
    private LobbyScreen lobbyScreen;
    private UsernameScreen usernameScreen;
    private final List<Player> currentPlayers = new ArrayList<>();
    private volatile boolean disconnectionAcknowledged = false;

    public GameStateManager(LudoGame game) {
        this.game = game;
        this.networkHandler = new ClientNetworkHandler();
        this.networkHandler.setMessageListener(this);
        this.isFirstPlayer = false;
        this.isMyTurn = false;
        this.gameStarted = false;
    }

    //for testing
    public GameStateManager() {
        this.game = null;
        this.networkHandler = new ClientNetworkHandler();
        this.networkHandler.setMessageListener(this);
        this.isFirstPlayer = false;
        this.gameStarted = false;
        this.isMyTurn = false;
    }

    //for testing
    public GameStateManager(ClientNetworkHandler networkHandler) {
        this.game = null;
        this.networkHandler = networkHandler;
        this.networkHandler.setMessageListener(this);
        this.isFirstPlayer = false;
        this.isMyTurn = false;
        this.gameStarted = false;
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
        
        if (event.isSuccess()) {
            gameScreen.playMoveAnimation(event.getPawnIndex(), event.getNewPosition());
            gameScreen.disableControls();
            currentDiceValue = 0;
        } else {
            LOGGER.warning("Move failed: " + event.getMessage());
            gameScreen.showMessage(event.getMessage());
            // Wait for server's GameStateUpdate to enable/disable controls
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

    private void handleDiceRoll(NetworkMessage message) {
        DiceRollResultEvent event = (DiceRollResultEvent) message;
        currentDiceValue = event.getValue();
        
        if (gameScreen != null) {
            gameScreen.updateDiceDisplay(currentDiceValue);
            // Wait for server's GameStateUpdate to enable/disable controls
        }
    }

    private void handleGameStateUpdate(NetworkMessage message) {
        GameStateUpdateEvent event = (GameStateUpdateEvent) message;

        if (gameScreen != null) {
            // Update pawn positions
            event.getPawnPositions().forEach((color, positions) ->
                gameScreen.updatePlayerPawns(color, positions)
            );

            // Update current player
            gameScreen.setCurrentPlayer(event.getCurrentPlayer());
            isMyTurn = event.getCurrentPlayer().equals(currentUsername);

            // Update game state
            setGameState(event.getGameState());

            // Update UI controls based on server state
            if (isMyTurn && event.getGameState() == GameState.WAITING_FOR_MOVE) {
                gameScreen.enablePawnSelection();
            } else if (isMyTurn && event.getGameState() == GameState.IN_PROGRESS) {
                gameScreen.enableRollButton();
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

        if (gameScreen == null) {
            LOGGER.warning("GameScreen is null when handling turn change");
            return;
        }

        gameScreen.disableControls();
        gameScreen.setCurrentPlayer(currentPlayer.getColor());

        if (isMyTurn) {
            gameScreen.enableRollButton();
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

                    // Create new game screen and set current player
                    GameScreen newGameScreen = new GameScreen(game);
                    newGameScreen.setCurrentPlayer(event.getSaveData().currentPlayerColor);
                    game.setScreen(newGameScreen);
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
