package ludo.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import ludo.client.networking.ClientNetworkHandler;
import ludo.client.screens.GameScreen;
import ludo.client.screens.LobbyScreen;
import ludo.client.screens.UsernameScreen;
import ludo.core.entities.Player;
import ludo.core.events.clientToServer.*;
import ludo.core.events.serverToClient.*;
import ludo.core.game.GameState;
import ludo.core.network.MessageListener;
import ludo.core.network.NetworkMessage;
import ludo.core.persistence.GamePersistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Manages the client's state and communication with the server.
 * It does not contain game logic.
 * It sends user requests to the server and updates the UI based on authoritative events from the server.
 */
public class GameStateManager implements MessageListener {
    private static final Logger LOGGER = Logger.getLogger(GameStateManager.class.getName());

    private final LudoGame game;
    private final ClientNetworkHandler networkHandler;
    private GameScreen gameScreen;
    private LobbyScreen lobbyScreen;

    // State required for UI and requests
    private String currentUsername;
    private String currentColor;
    private boolean isMyTurn = false;
    private int currentDiceValue = 0;
    private boolean isFirstPlayer = false;
    private boolean gameStarted = false;
    private final List<Player> currentPlayers = new ArrayList<>();
    private volatile boolean disconnectionAcknowledged = false;


    public GameStateManager(LudoGame game) {
        this.game = game;
        this.networkHandler = new ClientNetworkHandler();
        this.networkHandler.setMessageListener(this);
    }

    public boolean connect(String ip, int port) {
        try {
            networkHandler.connect(ip, port);
            return networkHandler.isConnected();
        } catch (Exception e) {
            LOGGER.severe("Connection failed: " + e.getMessage());
            return false;
        }
    }

    public void joinGame(String username, String color) {
        if (!networkHandler.isConnected()) {
            LOGGER.warning("Cannot join game - not connected to server");
            return;
        }
        this.currentUsername = username;
        this.currentColor = color;
        networkHandler.sendMessage(new JoinGameRequestEvent(username, color));
    }

//    public void startGame(boolean enableBots, boolean loadSavedGame) {
//        if (!isFirstPlayer || gameStarted) {
//            LOGGER.warning("Invalid game start request - isFirstPlayer: " + isFirstPlayer + ", gameStarted: " + gameStarted);
//            return;
//        }
//        String saveFileName = null;
//        if (loadSavedGame) {
//            // This logic is fine, as the client can know about local save files.
//            List<String> saveFiles = GamePersistence.listSaveFiles();
//            if (!saveFiles.isEmpty()) {
//                saveFileName = saveFiles.get(0); // Get most recent
//            } else {
//                LOGGER.warning("Load game requested, but no save files found.");
//                if (lobbyScreen != null) {
//                    lobbyScreen.showError("No save files found.");
//                }
//                return;
//            }
//        }
//        networkHandler.sendMessage(new StartGameRequestEvent(enableBots, loadSavedGame, saveFileName));
//    }

    public void startGame(boolean enableBots, boolean loadSavedGame, String saveFileName) {
        if (isFirstPlayer && !gameStarted) {
            LOGGER.info("First player requesting game start with bots: " + enableBots +
                ", load saved game: " + loadSavedGame + ", file: " + saveFileName);
            networkHandler.sendMessage(new StartGameRequestEvent(enableBots, loadSavedGame, saveFileName));
        } else {
            LOGGER.warning("Invalid game start request - isFirstPlayer: " +
                isFirstPlayer + ", gameStarted: " + gameStarted);
        }
    }

    public void startGame(boolean enableBots, boolean loadSavedGame) {
        // This path is for starting a NEW game, so the filename is null.
        startGame(enableBots, loadSavedGame, null);
    }

    public void requestDiceRoll() {
        if (!isMyTurn) {
            LOGGER.warning("Attempted to roll dice when not player's turn.");
            return;
        }
        networkHandler.sendMessage(new DiceRollRequestEvent());
        if(gameScreen != null) gameScreen.disableControls(); // UI feedback: disable controls while waiting for server
    }

    public void requestMove(int pawnIndex) {
        if (!isMyTurn || currentDiceValue == 0) {
            LOGGER.warning("Invalid move request - Not my turn or no dice roll.");
            if(gameScreen != null) gameScreen.showMessage("Not your turn or no dice roll value.");
            return;
        }
        if(gameScreen != null) gameScreen.highlightPawnAsMoving(pawnIndex);
        networkHandler.sendMessage(new MoveRequestEvent(pawnIndex, currentDiceValue));
        if(gameScreen != null) gameScreen.disableControls(); // UI feedback: disable controls while waiting for server
    }

    public void requestSaveGame(boolean isAutoSave) {
        if (isFirstPlayer && gameStarted) {
            networkHandler.sendMessage(new SaveGameRequestEvent(isAutoSave));
        }
    }

    public void leaveGame() {
        if(networkHandler.isConnected()) {
            networkHandler.sendMessage(new LeaveGameRequestEvent());
            networkHandler.disconnect();
        }
        resetClientState();
    }

    // --- Event Handlers from Server ---

    @Override
    public void onMessageReceived(NetworkMessage message) {
        // Always post UI updates to the main LibGDX thread
        Gdx.app.postRunnable(() -> {
            try {
                switch (message.getType()) {
                    case "FIRST_PLAYER":
                        handleFirstPlayer();
                        break;
                    case "JOIN_GAME_RESPONSE":
                        handleJoinResponse((JoinGameResponseEvent) message);
                        break;
                    case "DICE_ROLL_RESULT":
                        handleDiceRollResult((DiceRollResultEvent) message);
                        break;
                    case "MOVE_RESULT":
                        handleMoveResult((MoveResultEvent) message);
                        break;
                    case "GAME_STATE_UPDATE":
                        handleGameStateUpdate((GameStateUpdateEvent) message);
                        break;
                    case "PLAYER_JOINED":
                        // This event is informational. The WAITING_ROOM_UPDATE provides the full list.
                        LOGGER.info("A player joined. Waiting for lobby update.");
                        break;
                    case "TURN_CHANGE":
                        handleTurnChange((TurnChangeEvent) message);
                        break;
                    case "GAME_OVER":
                        handleGameOver((GameOverEvent) message);
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
                        handleErrorEvent((ErrorEvent) message);
                        break;
                    case "CAN_ROLL_AGAIN":
                        handleCanRollAgain();
                        break;
                    case "DISCONNECTION_ACKNOWLEDGED":
                        // This can be used for a more graceful shutdown sequence if needed
                        LOGGER.info("Server acknowledged disconnection.");
                        break;
                    case "SAVE_GAME_RESPONSE":
                        handleSaveGameResponse((SaveGameResponseEvent) message);
                        break;
                    case "LOAD_GAME_RESPONSE":
                        handleLoadGameResponse((LoadGameResponseEvent) message);
                        break;
                    case "SAVE_FILES_LIST":
                        handleSaveFilesList((SaveFilesListEvent) message);
                        break;
                    default:
                        LOGGER.warning("Unhandled message type: " + message.getType());
                }
            } catch (Exception e) {
                LOGGER.severe("Error processing message type " + message.getType() + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // --- Helper Methods for Event Handling ---

    private void handleGameStateUpdate(GameStateUpdateEvent event) {
        if (gameScreen == null) return;

        event.getPawnPositions().forEach(gameScreen::updatePlayerPawns);
        isMyTurn = event.getCurrentPlayer().equals(currentUsername);

        // Find the player color to update the HUD text correctly
        String currentPlayerColor = currentPlayers.stream()
            .filter(p -> p.getName().equals(event.getCurrentPlayer()))
            .findFirst()
            .map(Player::getColor)
            .orElse("Unknown");
        gameScreen.setCurrentPlayer(currentPlayerColor);

        // This logic now works correctly because isMyTurn is reliable
        if (isMyTurn) {
            if (event.getGameState() == GameState.WAITING_FOR_MOVE) {
                gameScreen.enablePawnSelection();
                gameScreen.showMessage("Select a pawn to move.");
            } else if (event.getGameState() == GameState.IN_PROGRESS) {
                gameScreen.enableRollButton();
                gameScreen.showMessage("Your turn! Roll the dice.");
            } else {
                gameScreen.disableControls();
            }
        } else {
            gameScreen.disableControls();
            gameScreen.showMessage("Waiting for " + currentPlayerColor);
        }
    }

    private void handleGameStarted(GameStartedEvent event) {
        gameStarted = true;
        currentPlayers.clear();
        currentPlayers.addAll(event.getPlayers());

        GameScreen newGameScreen = new GameScreen(game);
        setGameScreen(newGameScreen);

        newGameScreen.addPlayers(currentPlayers);

        String startingPlayerName = event.getStartingPlayer();
        isMyTurn = currentUsername.equals(startingPlayerName);

        String startingPlayerColor = currentPlayers.stream()
            .filter(p -> p.getName().equals(startingPlayerName))
            .findFirst()
            .map(Player::getColor)
            .orElse("");

        newGameScreen.setCurrentPlayer(startingPlayerColor);

        // This is the first state setting. It will be immediately updated
        // by the subsequent GameStateUpdateEvent from the server.
        if (isMyTurn) {
            newGameScreen.enableControls();
            newGameScreen.showMessage("Your turn!");
        } else {
            newGameScreen.disableControls();
            newGameScreen.showMessage("Waiting for " + startingPlayerColor);
        }

        game.setScreen(newGameScreen);
    }

    private void handleTurnChange(TurnChangeEvent event) {
        isMyTurn = event.getCurrentPlayer().equals(currentUsername);
        currentDiceValue = 0;

        if (gameScreen != null) {
            String color = currentPlayers.stream()
                .filter(p -> p.getName().equals(event.getCurrentPlayer()))
                .findFirst().map(Player::getColor).orElse("Unknown");

            gameScreen.setCurrentPlayer(color);
            if (isMyTurn) {
                gameScreen.enableRollButton();
                gameScreen.showMessage("Your turn! Roll the dice.");
            } else {
                gameScreen.disableControls();
                gameScreen.showMessage("Waiting for " + color);
            }
        }
    }

    private void handleDiceRollResult(DiceRollResultEvent event) {
        this.currentDiceValue = event.getValue();
        if (gameScreen != null) {
            gameScreen.updateDiceDisplay(currentDiceValue);
        }
    }

    private void handleMoveResult(MoveResultEvent event) {
        if (gameScreen == null) return;
        if (event.isSuccess()) {
            gameScreen.playMoveAnimation(event.getPawnIndex(), event.getNewPosition());
        } else {
            gameScreen.showMessage("Move failed: " + event.getMessage());
        }
    }

    private void handleCanRollAgain() {
        if (gameScreen != null && isMyTurn) {
            gameScreen.enableRollButton();
            gameScreen.showMessage("You get to roll again!");
        }
    }

    private void handleStartGameResponse(StartGameResponseEvent event) {
        if (!event.isSuccess()) {
            LOGGER.warning("Server rejected start game request: " + event.getMessage());
            if (lobbyScreen != null) {
                lobbyScreen.showError(event.getMessage());
                // Re-enable the start button if the request failed
                lobbyScreen.updateStartButtonState();
            }
        }
    }

    private void handleJoinResponse(JoinGameResponseEvent event) {
        if (event.getResponse() == Response.OK || event.getResponse() == Response.FIRST_PLAYER) {
            if (event.getResponse() == Response.FIRST_PLAYER) isFirstPlayer = true;
            game.setScreen(new LobbyScreen(game));
        } else {
            Screen currentScreen = game.getScreen();
            if(currentScreen instanceof UsernameScreen) {
                ((UsernameScreen)currentScreen).onJoinResponse(event.getResponse());
            }
        }
    }

    private void handleWaitingRoomUpdate(WaitingRoomUpdateEvent event) {
        if (lobbyScreen == null) return;
        currentPlayers.clear();
        for (String username : event.getUsernames()) {
            currentPlayers.add(new Player(username, event.getColorForPlayer(username)));
        }
        lobbyScreen.updatePlayersList(currentPlayers);
    }

    private void handleFirstPlayer() {
        isFirstPlayer = true;
        if (lobbyScreen != null) {
            lobbyScreen.setFirstPlayer();
        }
    }

    private void handleErrorEvent(ErrorEvent event) {
        LOGGER.warning("Error from server: " + event.getMessage());
        if (gameScreen != null) {
            gameScreen.showMessage("Error: " + event.getMessage());
            // The server state is king. A GameStateUpdate will follow to correct the UI.
        } else if (lobbyScreen != null) {
            lobbyScreen.showError(event.getMessage());
        }
    }

    private void handleGameOver(GameOverEvent event) {
        if (gameScreen != null) {
            gameScreen.showWinnerScreen(event.getWinner());
            gameScreen.disableControls();
        }
    }

    private void handleSaveGameResponse(SaveGameResponseEvent event) {
        if (event.getResponse() == SaveGameResponseEvent.Response.OK) {
            if (gameScreen != null) gameScreen.showMessage("Game saved successfully.");
        } else {
            if (gameScreen != null) gameScreen.showMessage("Save failed: " + event.getErrorMessage());
        }
    }

    private void handleLoadGameResponse(LoadGameResponseEvent event) {
        if (event.getResponse() != LoadGameResponseEvent.Response.OK) {
            if (lobbyScreen != null) lobbyScreen.showError("Load failed: " + event.getErrorMessage());
        }
        // On success, we do nothing here. The server will follow up with GameStartedEvent.
    }

    private void handleSaveFilesList(SaveFilesListEvent event) {
        List<String> saveFiles = event.getSaveFiles();
        LOGGER.info("Received " + saveFiles.size() + " save files from server.");
        if (lobbyScreen != null) {
            // Use Gdx.app.postRunnable to ensure UI updates happen on the main render thread
            Gdx.app.postRunnable(() -> lobbyScreen.showLoadGameDialog(saveFiles));
        } else {
            LOGGER.warning("LobbyScreen is null, cannot display save files list.");
        }
    }

    public void requestSaveFilesList() {
        LOGGER.info("Client is requesting the list of save files from the server.");
        networkHandler.sendMessage(new RequestSaveFilesEvent());
    }

//    private void handleWaitingRoomUpdate(WaitingRoomUpdateEvent event) {
//        if (lobbyScreen == null) return;
//        currentPlayers.clear();
//        for (int i=0; i < event.getUsernames().size(); i++) {
//            String username = event.getUsernames().get(i);
//            String color = event.getColorForPlayer(username);
//            currentPlayers.add(new Player(username, color));
//        }
//        lobbyScreen.updatePlayersList(currentPlayers);
//    }

    // --- Getters, Setters, and Utility ---

    private void resetClientState() {
        isMyTurn = false;
        currentDiceValue = 0;
        isFirstPlayer = false;
        gameStarted = false;
        currentPlayers.clear();
        gameScreen = null;
        lobbyScreen = null;
    }

    @Override
    public void onConnectionError(Exception error) {
        LOGGER.severe("Connection error: " + error.getMessage());
        if(game.getScreen() instanceof GameScreen || game.getScreen() instanceof LobbyScreen) {
            // Show error and transition back to connection screen
        }
    }

    public void dispose() {
        leaveGame();
    }

    public boolean isGameStarted() { return gameStarted; }
    public boolean isFirstPlayer() { return isFirstPlayer; }
    public String getCurrentUsername() { return currentUsername; }
    public String getCurrentColor() { return currentColor; }
    public boolean isConnected() { return networkHandler.isConnected(); }
    public List<Player> getCurrentPlayers() { return new ArrayList<>(currentPlayers); }

    public void setGameScreen(GameScreen screen) { this.gameScreen = screen; }
    public void setLobbyScreen(LobbyScreen screen) { this.lobbyScreen = screen; }

    public void resetGame() {
        resetClientState();
        if (gameScreen != null) {
            gameScreen.dispose();
            gameScreen = null;
        }
        if (lobbyScreen != null) {
            lobbyScreen.dispose();
            lobbyScreen = null;
        }
        networkHandler.reset(); // Reset the network handler state
        disconnectionAcknowledged = false; // Reset disconnection state
    }

    public void sendMessage(NetworkMessage message) {
        if (networkHandler != null && networkHandler.isConnected()) {
            networkHandler.sendMessage(message);
        }
    }

    public void disconnect() {
        if (networkHandler != null) {
            networkHandler.disconnect();
        }
    }
}
