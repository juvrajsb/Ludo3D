package ludo.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import ludo.client.screens.LobbyScreen;
import ludo.client.screens.UsernameScreen;
import ludo.core.entities.Pawn;
import ludo.core.events.serverToClient.*;
import ludo.core.events.clientToServer.*;
import ludo.core.network.*;
import ludo.client.networking.ClientNetworkHandler;
import ludo.client.screens.GameScreen;
import ludo.core.entities.Player;

import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.List;

public class GameStateManager implements MessageListener {
    private static final Logger LOGGER = Logger.getLogger(GameStateManager.class.getName());

    private final ClientNetworkHandler networkHandler;
    private GameScreen gameScreen;
    private boolean isMyTurn;
    private int currentDiceValue;
    private boolean isFirstPlayer;
    private boolean gameStarted;
    private String currentUsername;
    private String currentColor;
    private Screen currentScreen;
    private LobbyScreen lobbyScreen;
    private UsernameScreen usernameScreen;
    private final LudoGame game;
    private final List<Player> currentPlayers = new ArrayList<>();

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
        LOGGER.info("Attempting to connect to " + ip + ":" + port);
        try {
            networkHandler.connect(ip, port);
            boolean connected = networkHandler.isConnected();
            if (connected) {
                LOGGER.info("Successfully connected to server");
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
            LOGGER.info("Join request sent successfully");
            return true;
        } catch (Exception e) {
            LOGGER.severe("Failed to send join request: " + e.getMessage());
            return false;
        }
    }

    public void checkFirstPlayer(MessageListener callback) { //TODO check usage not used currently
        if (isFirstPlayer) {
            callback.onMessageReceived(new FirstPlayerEvent());
        }
    }

    public boolean isFirstPlayer() {
        return isFirstPlayer;
    }

    public void startGame() {
        if (isFirstPlayer && !gameStarted) {
            LOGGER.info("First player requesting game start");
            networkHandler.sendMessage(new StartGameRequestEvent());
        } else {
            LOGGER.warning("Invalid game start request - isFirstPlayer: " +
                isFirstPlayer + ", gameStarted: " + gameStarted);
        }
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
                    LOGGER.info("Join response received: " + joinResponse.getResponse());
                    handleJoinResponse(joinResponse);
                    break;
                case "DICE_ROLL_RESULT":
                    DiceRollResultEvent diceEvent = (DiceRollResultEvent) message;
                    LOGGER.info("Dice roll result: " + diceEvent.getValue() + ", Color: " + diceEvent.getPlayerColor());
                    handleDiceRoll(message);
                    break;
                case "MOVE_RESULT":
                    MoveResultEvent moveEvent = (MoveResultEvent) message;
                    LOGGER.info("Move result - Success: " + moveEvent.isSuccess() +
                        ", PawnIndex: " + moveEvent.getPawnIndex() +
                        ", NewPosition: " + moveEvent.getNewPosition());
                    handleMoveResult(message);
                    break;
                case "GAME_STATE_UPDATE":
                    LOGGER.info("Game state update received");
                    handleGameStateUpdate(message);
                    logGameState((GameStateUpdateEvent) message);
                    break;
                case "PLAYER_JOINED":
                    PlayerJoinedEvent joinEvent = (PlayerJoinedEvent) message;
                    LOGGER.info("Player joined: " + joinEvent.getPlayer().getName() +
                        " (" + joinEvent.getPlayer().getColor() + ")");
                    handlePlayerJoined(message);
                    break;
                case "TURN_CHANGE":
                    TurnChangeEvent turnEvent = (TurnChangeEvent) message;
                    LOGGER.info("Turn changed to: " + turnEvent.getCurrentPlayer() +
                        " (isMyTurn: " + turnEvent.getCurrentPlayer().equals(currentUsername) + ")");
                    handleTurnChange(message);
                    break;
                case "GAME_OVER":
                    GameOverEvent gameOverEvent = (GameOverEvent) message;
                    LOGGER.info("Game over - Winner: " + gameOverEvent.getWinner());
                    handleGameOver(message);
                    break;
                case "GAME_STARTED":
                    LOGGER.info("Game start event received");
                    handleGameStarted((GameStartedEvent) message);
                    break;
                case "START_GAME_RESPONSE":
                    handleStartGameResponse((StartGameResponseEvent) message);
                    break;
                case "WAITING_ROOM_UPDATE":
                    WaitingRoomUpdateEvent roomEvent = (WaitingRoomUpdateEvent) message;
                    LOGGER.info("Waiting room update - Players: " + roomEvent.getUsernames() +
                        " (" + roomEvent.getNumberOfPlayers() + " players)");
                    handleWaitingRoomUpdate(roomEvent);
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
            return;
        }

        LOGGER.info("Requesting move - Pawn: " + pawnIndex + ", Steps: " + currentDiceValue);
        networkHandler.sendMessage(new MoveRequestEvent(pawnIndex, currentDiceValue));
    }

    private void handleMoveResult(NetworkMessage message) {
        MoveResultEvent event = (MoveResultEvent) message;
        if (event.isSuccess()) {
//            gameScreen.updatePawnPosition(event.getPawnIndex(), event.getNewPosition());
            gameScreen.playMoveAnimation(event.getPawnIndex(), event.getNewPosition());
        } else {
            gameScreen.showMessage(event.getMessage());
        }
    }

    public void endTurn() {
        if (isMyTurn) {
            networkHandler.sendMessage(new TurnEndEvent());
            isMyTurn = false;
        }
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
        LOGGER.info("Setting first player flag from first player event");
        isFirstPlayer = true;
        // If we're already in lobby screen, update it
        if (lobbyScreen != null) {
            lobbyScreen.setFirstPlayer();
        }
    }

    public void handleWaitingRoomUpdate(WaitingRoomUpdateEvent event) {
        LOGGER.info("Updating lobby with " + event.getUsernames().size() + " players");

        // Store players in GameStateManager
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
                LOGGER.info("Setting first player status on new lobby screen");
                screen.setFirstPlayer();
            }
        }
    }

    private void handleJoinResponse(JoinGameResponseEvent event) {
        LOGGER.info("Join response received: " + event.getResponse());

        if (event.getResponse() == Response.FIRST_PLAYER) {
            isFirstPlayer = true;
        }

        if (currentScreen instanceof UsernameScreen) {
            Gdx.app.postRunnable(() -> {
                UsernameScreen screen = (UsernameScreen) currentScreen;
                screen.onJoinResponse(event.getResponse());
            });
        }
    }

    public void handleGameStarted(GameStartedEvent event) {
        LOGGER.info("Game started event received");
        LOGGER.info("Current players in GameStateManager: " + currentPlayers.size());
        LOGGER.info("Players received in event: " + event.getPlayers().size());

        if (gameScreen != null) {
            LOGGER.info("Game screen exists, skipping creation");
            return;
        }

        Gdx.app.postRunnable(() -> {
            GameScreen newGameScreen = new GameScreen(game);
            this.gameScreen = newGameScreen;

            event.getPlayers().forEach(player -> {
                LOGGER.info("Adding player to game: " + player.getName());
                newGameScreen.addPlayer(player);
            });

            // Use player name instead of color for comparison
            String startingPlayerName = event.getStartingPlayer();
            newGameScreen.setCurrentPlayer(startingPlayerName);
            isMyTurn = currentUsername.equals(startingPlayerName);

            if (isMyTurn) {
                newGameScreen.enableControls();
                newGameScreen.showMessage("Your turn!");
            } else {
                newGameScreen.disableControls();
                newGameScreen.showMessage("Waiting for " + startingPlayerName);
            }

            game.setScreen(newGameScreen);
            LOGGER.info("Screen transition complete. Current player: " + currentUsername + 
                ", isMyTurn: " + isMyTurn);
        });
    }

    public List<Player> getCurrentPlayers() {
        return new ArrayList<>(currentPlayers);
    }

    public void handleStartGameResponse(StartGameResponseEvent event) {
        LOGGER.info("Start game response received: " + event.getResponse());

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

    public void handleDiceRoll(NetworkMessage message) {
        DiceRollResultEvent event = (DiceRollResultEvent) message;
        currentDiceValue = event.getValue();
        LOGGER.info("Dice roll result: " + currentDiceValue + " for " + event.getPlayerColor());

        gameScreen.updateDiceDisplay(currentDiceValue);

        // Add this block to properly enable pawn selection
        if (isMyTurn) {
            // Check if it's a valid roll
            boolean hasValidMove = false;
            Player currentPlayer = gameScreen.getCurrentPlayer();
            if (currentPlayer != null) {
                if (currentDiceValue == 6) {
                    // Can move from home or existing pawns
                    hasValidMove = true;
                } else {
                    // Check if player has any pawns outside home
                    for (Pawn pawn : currentPlayer.getPawns()) {
                        if (!pawn.isHome()) {
                            hasValidMove = true;
                            break;
                        }
                    }
                }
            }

            LOGGER.info("Valid move check: " + hasValidMove + " for player " + currentPlayer.getName()+ " with roll: " + currentDiceValue);

            if (hasValidMove) {
                gameScreen.enablePawnSelection();
                gameScreen.showMessage("Select a pawn to move");
            } else {
                LOGGER.info("No valid moves available with roll: " + currentDiceValue);
                gameScreen.showMessage("No valid moves - turn passed");
                networkHandler.sendMessage(new TurnEndEvent());
            }
        }
    }

    public void setCurrentPlayer(String color) {
        if (gameScreen != null) {
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

    public void handleGameStateUpdate(NetworkMessage message) {
        GameStateUpdateEvent event = (GameStateUpdateEvent) message;

        if (gameScreen != null) {
            event.getPawnPositions().forEach((color, positions) ->
                gameScreen.updatePlayerPawns(color, positions)
            );

            gameScreen.setCurrentPlayer(event.getCurrentPlayer());

            // Update message based on whose turn it is
            if (isMyTurn) {
                gameScreen.showMessage("Your turn! " +
                    (currentDiceValue > 0 ? "Select a pawn to move" : "Roll the dice"));
            } else {
                gameScreen.showMessage("Waiting for " + event.getCurrentPlayer() + "'s move");
            }
        }
    }

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
        isMyTurn = event.getCurrentPlayer().equals(currentUsername);
        LOGGER.info("Turn changed to: " + event.getCurrentPlayer() + " (isMyTurn: " + isMyTurn + ")");
        currentDiceValue = 0;

        if (gameScreen != null) {
            if (isMyTurn) {
                LOGGER.info("Beginning turn for " + currentUsername);
                gameScreen.enableControls();
                gameScreen.showMessage("Your turn! Roll the dice");
            } else {
                gameScreen.disableControls();
                gameScreen.showMessage("Waiting for " + event.getCurrentPlayer());
            }
        }
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

    public void handlePlayerLeft(String playerName) { //TODO check usage not used currently
        gameScreen.removePlayer(playerName);
    }

    public GameScreen getGameScreen() { //TODO check usage not used currently
        return gameScreen;
    }

    public NetworkHandler getNetworkHandler() { //TODO check usage not used currently
        return networkHandler;
    }

    public void addPlayer(Player player) { //TODO check usage not used currently
        gameScreen.addPlayer(player);
    }

    public boolean isConnected() {
        return networkHandler.isConnected();
    }
}
