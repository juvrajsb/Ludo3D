package ludo.client;

import ludo.core.events.serverToClient.*;
import ludo.core.events.clientToServer.*;
import ludo.core.game.GameState;
import ludo.core.network.*;
import ludo.client.networking.ClientNetworkHandler;
import ludo.client.screens.GameScreen;
import ludo.core.entities.Player;
import ludo.core.events.*;

public class GameStateManager implements MessageListener {
    private final ClientNetworkHandler networkHandler;
    private GameScreen gameScreen;
    private boolean isMyTurn;
    private int currentDiceValue;
    private boolean isFirstPlayer;
    private boolean gameStarted;
    private String currentUsername;
    private String currentColor;

    public GameStateManager() {
        this.networkHandler = new ClientNetworkHandler();
        this.networkHandler.setMessageListener(this);
        this.isFirstPlayer = false;
        this.gameStarted = false;
    }

    public boolean connect(String ip, int port) {
        try {
            networkHandler.connect(ip, port);
            return networkHandler.isConnected();
        } catch (Exception e) {
            System.err.println("Connection failed: " + e.getMessage());
            return false;
        }
    }

    public boolean joinGame(String username, String color) {
        if (!networkHandler.isConnected()) {
            return false;
        }

        this.currentUsername = username;
        this.currentColor = color;

        JoinGameRequestEvent joinRequest = new JoinGameRequestEvent(username, color);
        try {
            networkHandler.sendMessage(joinRequest);
            return true; // Return true initially, actual result will come through message
        } catch (Exception e) {
            System.err.println("Failed to send join request: " + e.getMessage());
            return false;
        }
    }

    public boolean isFirstPlayer() {
        return isFirstPlayer;
    }

    public void startGame() {
        if (isFirstPlayer && !gameStarted) {
            networkHandler.sendMessage(new StartGameRequestEvent());
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

    public void initialize(GameScreen gameScreen) {
        this.gameScreen = gameScreen;
    }

    @Override
    public void onMessageReceived(NetworkMessage message) {
        switch (message.getType()) {
            case "JOIN_GAME_RESPONSE":
                handleJoinResponse((JoinGameResponseEvent) message);
                break;
            case "DICE_ROLL_RESULT":
                handleDiceRoll(message);
                break;
            case "MOVE_RESULT":
                handleMoveResult(message);
                break;
            case "GAME_STATE_UPDATE":
                handleGameStateUpdate(message);
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
            case "GAME_START":
                handleGameStart((GameStartEvent) message);
                break;
        }
    }

    @Override
    public void onConnectionError(Exception error) {
        if (gameScreen != null) {
            gameScreen.showMessage("Connection error: " + error.getMessage());
        }
    }

    private void handleJoinResponse(JoinGameResponseEvent event) {
        if (event.getResponse() == Response.FIRST_PLAYER) {
            isFirstPlayer = true;
        }
    }

    private void handleGameStart(GameStartEvent event) {
        gameStarted = true;
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

        // Update pawn positions for all players
        event.getPawnPositions().forEach((color, positions) ->
            gameScreen.updatePlayerPawns(color, positions)
        );

        gameScreen.setCurrentPlayer(event.getCurrentPlayer());
        gameScreen.updateGameState(event.getGameState().getDescription());
    }

    private void handlePlayerJoined(NetworkMessage message) {
        PlayerJoinedEvent event = (PlayerJoinedEvent) message;
        Player newPlayer = event.getPlayer();
        gameScreen.addPlayer(newPlayer);
    }

    private void handleTurnChange(NetworkMessage message) {
        TurnChangeEvent event = (TurnChangeEvent) message;
        isMyTurn = event.getCurrentPlayer().equals(networkHandler.getPlayerId());

        if (isMyTurn) {
            gameScreen.enableControls();
            gameScreen.showMessage("Your turn!");
        } else {
            gameScreen.disableControls();
            gameScreen.showMessage("Waiting for " + event.getCurrentPlayer());
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
            networkHandler.sendMessage(new DiceRollRequestEvent());
        }
    }

    public void requestMove(int pawnIndex) {
        if (isMyTurn && currentDiceValue > 0) {
            networkHandler.sendMessage(new MoveRequestEvent(pawnIndex, currentDiceValue));
            currentDiceValue = 0; // Reset after move
        }
    }

    public void dispose() {
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
}
