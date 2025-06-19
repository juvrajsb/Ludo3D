package ludo.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import ludo.client.handlers.GameStartedHandler;
import ludo.client.handlers.GameStateUpdateHandler;
import ludo.client.handlers.IClientEventHandler;
import ludo.client.networking.ClientNetworkHandler;
import ludo.client.screens.ConnectionScreen;
import ludo.client.screens.GameScreen;
import ludo.client.screens.LobbyScreen;
import ludo.client.screens.UsernameScreen;
import ludo.core.entities.Player;
import ludo.core.events.Event;
import ludo.core.events.clientToServer.*;
import ludo.core.events.serverToClient.*;
import ludo.core.network.MessageListener;
import ludo.core.network.NetworkMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class GameStateManager implements MessageListener {
    private static final Logger LOGGER = Logger.getLogger(GameStateManager.class.getName());

    private final LudoGame game;
    private final ClientNetworkHandler networkHandler;
    private final Map<String, IClientEventHandler> eventHandlers;

    // --- UI and State References ---
    private GameScreen gameScreen;
    private LobbyScreen lobbyScreen;
    private String currentUsername;
    private String currentColor;
    private boolean isMyTurn = false;
    private int currentDiceValue = 0;
    private boolean isFirstPlayer = false;
    private boolean gameStarted = false;
    private final List<Player> currentPlayers = new ArrayList<>();

    public GameStateManager(LudoGame game) {
        this.game = game;
        this.networkHandler = new ClientNetworkHandler();
        this.networkHandler.setMessageListener(this);
        this.eventHandlers = new HashMap<>();
        initializeEventHandlers();
    }

    private void initializeEventHandlers() {
        eventHandlers.put("GAME_STARTED", new GameStartedHandler());
        eventHandlers.put("GAME_STATE_UPDATE", new GameStateUpdateHandler());

        eventHandlers.put("JOIN_GAME_RESPONSE", (gsm, event) -> {
            JoinGameResponseEvent joinResponse = (JoinGameResponseEvent) event;
            if (joinResponse.getResponse() == Response.OK || joinResponse.getResponse() == Response.FIRST_PLAYER) {
                if (joinResponse.getResponse() == Response.FIRST_PLAYER) gsm.setFirstPlayer(true);
                gsm.getGame().setScreen(new LobbyScreen(gsm.getGame()));
            } else {
                Screen currentScreen = gsm.getGame().getScreen();
                if (currentScreen instanceof UsernameScreen) {
                    ((UsernameScreen) currentScreen).onJoinResponse(joinResponse.getResponse());
                }
            }
        });

        eventHandlers.put("START_GAME_RESPONSE", (gsm, event) -> {
            StartGameResponseEvent response = (StartGameResponseEvent) event;
            if (!response.isSuccess() && gsm.getLobbyScreen() != null) {
                gsm.getLobbyScreen().showError(response.getMessage());
                gsm.getLobbyScreen().updateStartButtonState();
            }
        });

        eventHandlers.put("TURN_CHANGE", (gsm, event) -> {
            TurnChangeEvent turnChangeEvent = (TurnChangeEvent) event;
            gsm.setMyTurn(turnChangeEvent.getCurrentPlayer().equals(gsm.getCurrentUsername()));
            gsm.setCurrentDiceValue(0);
            if (gsm.getGameScreen() == null) return;

            String color = gsm.getCurrentPlayers().stream()
                .filter(p -> p.getName().equals(turnChangeEvent.getCurrentPlayer()))
                .findFirst().map(Player::getColor).orElse("Unknown");

            gsm.getGameScreen().setCurrentPlayer(color);
            if (gsm.isMyTurn()) {
                gsm.getGameScreen().enableRollButton();
                gsm.getGameScreen().showMessage("Your turn! Roll the dice.");
            } else {
                gsm.getGameScreen().disableControls();
                gsm.getGameScreen().showMessage("Waiting for " + color);
            }
        });

        eventHandlers.put("DICE_ROLL_RESULT", (gsm, event) -> {
            DiceRollResultEvent rollResult = (DiceRollResultEvent) event;
            gsm.setCurrentDiceValue(rollResult.getValue());
            if (gsm.getGameScreen() != null) {
                gsm.getGameScreen().updateDiceDisplay(rollResult.getValue());
            }
        });

        eventHandlers.put("MOVE_RESULT", (gsm, event) -> {
            MoveResultEvent moveResult = (MoveResultEvent) event;
            if (gsm.getGameScreen() == null) return;
            if (moveResult.isSuccess()) {
                gsm.getGameScreen().playMoveAnimation(moveResult.getPawnIndex(), moveResult.getNewPosition());
            } else {
                gsm.getGameScreen().showMessage("Move failed: " + moveResult.getMessage());
            }
        });

        eventHandlers.put("CAN_ROLL_AGAIN", (gsm, event) -> {
            if (gsm.getGameScreen() != null && gsm.isMyTurn()) {
                gsm.getGameScreen().enableRollButton();
                gsm.getGameScreen().showMessage("You get to roll again!");
            }
        });

        eventHandlers.put("GAME_OVER", (gsm, event) -> {
            GameOverEvent gameOverEvent = (GameOverEvent) event;
            if (gsm.getGameScreen() != null) {
                gsm.getGameScreen().showWinnerScreen(gameOverEvent.getWinner());
                gsm.getGameScreen().disableControls();
            }
        });

        eventHandlers.put("WAITING_ROOM_UPDATE", (gsm, event) -> {
            WaitingRoomUpdateEvent updateEvent = (WaitingRoomUpdateEvent) event;
            if (gsm.getLobbyScreen() == null) return;
            gsm.getCurrentPlayers().clear();
            for (String username : updateEvent.getUsernames()) {
                gsm.getCurrentPlayers().add(new Player(username, updateEvent.getColorForPlayer(username)));
            }
            gsm.getLobbyScreen().updatePlayersList(gsm.getCurrentPlayers());
        });

        eventHandlers.put("PLAYER_LEFT", (gsm, event) -> {
            PlayerLeftEvent leftEvent = (PlayerLeftEvent) event;
            if (gsm.getGameScreen() != null) {
                gsm.getGameScreen().showMessage("Player " + leftEvent.getPlayerName() + " has left.");
            }
        });

        eventHandlers.put("RECONNECT_PROMPT", (gsm, event) -> {
            ReconnectPromptEvent promptEvent = (ReconnectPromptEvent) event;
            Screen currentScreen = gsm.getGame().getScreen();
            if (currentScreen instanceof UsernameScreen) {
                ((UsernameScreen) currentScreen).showReconnectDialog(promptEvent.getPlayerName());
            }
        });

        eventHandlers.put("UNEXPECTED_DISCONNECTION", (gsm, event) -> {
            gsm.getGame().setScreen(new ConnectionScreen(gsm.getGame()));
        });

        eventHandlers.put("SAVE_FILES_LIST", (gsm, event) -> {
            SaveFilesListEvent listEvent = (SaveFilesListEvent) event;
            if (gsm.getLobbyScreen() != null) {
                gsm.getLobbyScreen().showLoadGameDialog(listEvent.getSaveFiles());
            }
        });

        eventHandlers.put("SAVE_GAME_RESPONSE", (gsm, event) -> {
            SaveGameResponseEvent response = (SaveGameResponseEvent) event;
            if (gsm.getGameScreen() != null) {
                String message = response.getResponse() == SaveGameResponseEvent.Response.OK
                    ? "Game saved."
                    : "Save failed: " + response.getErrorMessage();
                gsm.getGameScreen().showMessage(message);
            }
        });

        eventHandlers.put("LOAD_GAME_RESPONSE", (gsm, event) -> {
            LoadGameResponseEvent response = (LoadGameResponseEvent) event;
            if (response.getResponse() != LoadGameResponseEvent.Response.OK && gsm.getLobbyScreen() != null) {
                gsm.getLobbyScreen().showError("Load failed: " + response.getErrorMessage());
            }
            // On success, do nothing; server follows up with GameStartedEvent.
        });

        eventHandlers.put("FIRST_PLAYER", (gsm, event) -> gsm.setFirstPlayer(true));
        eventHandlers.put("ERROR", (gsm, event) -> {
            ErrorEvent errorEvent = (ErrorEvent) event;
            LOGGER.warning("Error from server: " + errorEvent.getMessage());
            if (gsm.getGameScreen() != null) gsm.getGameScreen().showMessage("Error: " + errorEvent.getMessage());
            else if (gsm.getLobbyScreen() != null) gsm.getLobbyScreen().showError(errorEvent.getMessage());
        });
        eventHandlers.put("PLAYER_JOINED", (gsm, event) -> LOGGER.info("A player joined. Waiting for lobby update."));
        eventHandlers.put("DISCONNECTION_ACKNOWLEDGED", (gsm, event) -> LOGGER.info("Server acknowledged disconnection."));
    }

    @Override
    public void onMessageReceived(NetworkMessage message) {
        Gdx.app.postRunnable(() -> {
            if (message instanceof Event event) {
                IClientEventHandler handler = eventHandlers.get(event.getType());
                if (handler != null) {
                    try {
                        handler.handle(this, event);
                    } catch (Exception e) {
                        LOGGER.severe("Error processing event " + event.getType() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    LOGGER.warning("No client handler for event type: " + event.getType());
                }
            }
        });
    }

    public LudoGame getGame() { return game; }
    public GameScreen getGameScreen() { return gameScreen; }
    public void setGameScreen(GameScreen gameScreen) { this.gameScreen = gameScreen; }
    public LobbyScreen getLobbyScreen() { return lobbyScreen; }
    public void setLobbyScreen(LobbyScreen lobbyScreen) { this.lobbyScreen = lobbyScreen; }
    public String getCurrentUsername() { return currentUsername; }
    public List<Player> getCurrentPlayers() { return currentPlayers; }
    public boolean isMyTurn() { return isMyTurn; }
    public void setMyTurn(boolean myTurn) { isMyTurn = myTurn; }
    public void setCurrentDiceValue(int value) { this.currentDiceValue = value; }
    public void setFirstPlayer(boolean firstPlayer) {
        isFirstPlayer = firstPlayer;
        if (lobbyScreen != null) {
            lobbyScreen.setFirstPlayer();
        }
    }
    public void setGameStarted(boolean gameStarted) { this.gameStarted = gameStarted; }
    public boolean isFirstPlayer() { return isFirstPlayer; }
    public boolean isConnected() { return networkHandler.isConnected(); }
    public String getCurrentColor() { return currentColor; }
    public boolean isGameStarted() { return gameStarted; }

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

    public void startGame(boolean enableBots, boolean loadSavedGame) {
        startGame(enableBots, loadSavedGame, null);
    }

    public void startGame(boolean enableBots, boolean loadSavedGame, String saveFileName) {
        if (isFirstPlayer && !gameStarted) {
            networkHandler.sendMessage(new StartGameRequestEvent(enableBots, loadSavedGame, saveFileName));
        }
    }
    public void requestSaveGame(boolean isAutoSave) {
        if (isFirstPlayer && gameStarted) {
            networkHandler.sendMessage(new SaveGameRequestEvent(isAutoSave));
        }
    }
    public void requestDiceRoll() {
        if (!isMyTurn) return;
        networkHandler.sendMessage(new DiceRollRequestEvent());
        if(gameScreen != null) gameScreen.disableControls();
    }

    public void requestMove(int pawnIndex) {
        if (!isMyTurn || currentDiceValue == 0) return;
        networkHandler.sendMessage(new MoveRequestEvent(pawnIndex, currentDiceValue));
        if(gameScreen != null) gameScreen.disableControls();
    }

    public void requestSaveFilesList() {
        networkHandler.sendMessage(new RequestSaveFilesEvent());
    }

    public void sendReconnectRequest(String username) {
        networkHandler.sendMessage(new ReconnectRequestEvent(username));
    }

    public void leaveGame() {
        if(networkHandler.isConnected()) {
            networkHandler.sendMessage(new LeaveGameRequestEvent());
            networkHandler.disconnect();
        }
    }

    @Override
    public void onConnectionError(Exception e) {
        LOGGER.severe("Connection error: " + e.getMessage());
        Gdx.app.postRunnable(() -> game.setScreen(new ConnectionScreen(game)));
    }

    public void dispose() {
        leaveGame();
    }
}
