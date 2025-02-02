package ludo.client;

import com.badlogic.gdx.Screen;
import ludo.client.screens.LobbyScreen;
import ludo.core.events.serverToClient.*;
import ludo.core.events.clientToServer.*;
import ludo.core.game.GameState;
import ludo.core.network.*;
import ludo.client.networking.ClientNetworkHandler;
import ludo.client.screens.GameScreen;
import ludo.core.entities.Player;
import ludo.core.events.*;

import java.util.Objects;
import java.util.logging.Logger;
import java.util.List;
import java.util.stream.Collectors;

public class GameStateManager implements MessageListener {
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
    private static final Logger LOGGER = Logger.getLogger(GameStateManager.class.getName());


    public GameStateManager() {
        this.networkHandler = new ClientNetworkHandler();
        this.networkHandler.setMessageListener(this);
        this.isFirstPlayer = false;
        this.gameStarted = false;
    }

    //for testing
    public GameStateManager(ClientNetworkHandler networkHandler) {
        this.networkHandler = networkHandler;
        this.networkHandler.setMessageListener(this);
        this.isFirstPlayer = false;
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

    public void checkFirstPlayer(MessageListener callback) {
        // The actual first player check will happen through normal message handling
        // This is just to register the callback
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
//        if (message instanceof Event) {
//            Event event = (Event) message;
//            if (!Objects.equals(event.getType(), "PING")) {
//                LOGGER.info("Received message: " + message.getType());;
//            }
//        }
//            if(!Objects.equals(message.getType(), "PING")){LOGGER.info("Received message: " + message.getType());}

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

    private void handleStartGameResponse(StartGameResponseEvent event) {
        LOGGER.info("Start game response received: " + event.getResponse());

        if (event.isSuccess()) {
            gameStarted = true;
            LOGGER.info("Game successfully started");
        } else {
            LOGGER.warning("Failed to start game: " + event.getMessage());
            if (lobbyScreen != null) {
                lobbyScreen.showError("Failed to start game: " + event.getMessage());
            }
        }
    }

    private void handleWaitingRoomUpdate(WaitingRoomUpdateEvent event) {
        if (lobbyScreen != null) {
            List<Player> players = event.getUsernames().stream()
                .map(name -> new Player(name, event.getColorForPlayer(name)))
                .collect(Collectors.toList());

            LOGGER.info("Updating lobby with " + players.size() + " players");
            lobbyScreen.updatePlayersList(players);
        }
    }

    public void setCurrentScreen(Screen screen) {
        this.currentScreen = screen;
        if (screen instanceof GameScreen) {
            this.gameScreen = (GameScreen) screen;
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
        this.lobbyScreen = screen;
        // If this player was already designated as first player, update the new screen
        if (isFirstPlayer && screen != null) {
            LOGGER.info("Setting first player status on new lobby screen");
            screen.setFirstPlayer();
        }
    }

    private void handleJoinResponse(JoinGameResponseEvent event) {
        if (event.getResponse() == Response.FIRST_PLAYER) {
            LOGGER.info("Setting first player flag from join response");
            isFirstPlayer = true;
            // If we're already in lobby screen, update it
            if (lobbyScreen != null) {
                lobbyScreen.setFirstPlayer();
            }
        }
    }

    private void handleGameStarted(GameStartedEvent event) {
        LOGGER.info("Game started event received");
        gameStarted = true;

        // Initialize game with the provided players and starting player
        if (gameScreen != null) {
            for (Player player : event.getPlayers()) {
                gameScreen.addPlayer(player);
            }
            gameScreen.setCurrentPlayer(event.getStartingPlayer());
        }
    }

    public void handleDiceRoll(NetworkMessage message) {
        DiceRollResultEvent event = (DiceRollResultEvent) message; //todo: check if this is correct
        currentDiceValue = event.getValue();
        gameScreen.updateDiceDisplay(currentDiceValue);

        if (isMyTurn) {
            gameScreen.enablePawnSelection();
        }
    }

    public void handleMoveResult(NetworkMessage message) {
        MoveResultEvent event = (MoveResultEvent) message;
        if (event.isSuccess()) {
            gameScreen.updatePawnPosition(event.getPawnIndex(), event.getNewPosition());
            gameScreen.playMoveAnimation(event.getPawnIndex(), event.getNewPosition());
        } else {
            gameScreen.showMessage(event.getMessage());
        }
    }

    public void handleGameStateUpdate(NetworkMessage message) {
        GameStateUpdateEvent event = (GameStateUpdateEvent) message;

        // Only process if we're in game screen
        if (gameScreen != null) {
            // Update pawn positions for all players
            event.getPawnPositions().forEach((color, positions) ->
                gameScreen.updatePlayerPawns(color, positions)
            );

            gameScreen.setCurrentPlayer(event.getCurrentPlayer());
            gameScreen.updateGameState(event.getGameState().getDescription());
        }
    }

    private void handlePlayerJoined(NetworkMessage message) {
        PlayerJoinedEvent event = (PlayerJoinedEvent) message;
        Player newPlayer = event.getPlayer();

        // Handle based on current screen
        if (gameScreen != null) {
            gameScreen.addPlayer(newPlayer);
        } else if (lobbyScreen != null) {
            lobbyScreen.addPlayer(newPlayer);
        }
    }

    private void handleTurnChange(NetworkMessage message) {
        TurnChangeEvent event = (TurnChangeEvent) message;
        isMyTurn = event.getCurrentPlayer().equals(currentUsername);

        if (gameScreen != null) {
            if (isMyTurn) {
                gameScreen.enableControls();
                gameScreen.showMessage("Your turn!");
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

    public String getCurrentUsername() {
        return currentUsername;
    }

    public String getCurrentColor() {
        return currentColor;
    }

    // Methods called by GameScreen
    public void requestDiceRoll() {
        if (isMyTurn) {
            LOGGER.info("Requesting dice roll");
            networkHandler.sendMessage(new DiceRollRequestEvent());
        } else {
            LOGGER.warning("Attempted to roll dice when not player's turn");
        }
    }

    public void requestMove(int pawnIndex) {
        if (isMyTurn && currentDiceValue > 0) {
            LOGGER.info("Requesting move - PawnIndex: " + pawnIndex + ", Steps: " + currentDiceValue);
            networkHandler.sendMessage(new MoveRequestEvent(pawnIndex, currentDiceValue));
            currentDiceValue = 0;
        } else {
            LOGGER.warning("Invalid move request - isMyTurn: " + isMyTurn +
                ", currentDiceValue: " + currentDiceValue);
        }
    }

    public void dispose() {
        LOGGER.info("Disposing GameStateManager");
        if (networkHandler != null) {
            networkHandler.disconnect();
        }
    }

    public void handlePlayerLeft(String playerName) {
        gameScreen.removePlayer(playerName);
    }

    public GameScreen getGameScreen() {
        return gameScreen;
    }

    public NetworkHandler getNetworkHandler() {
        return networkHandler;
    }

    public void addPlayer(Player player) {
        gameScreen.addPlayer(player);
    }

    public boolean isConnected() {
        return networkHandler.isConnected();
    }
}
