package ludo.client;

import ludo.core.events.serverToClient.*;
import ludo.core.events.clientToServer.*;

import ludo.core.game.GameState;
import ludo.core.network.*;
import ludo.client.networking.ClientNetworkHandler;
import ludo.client.screens.GameScreen;
import ludo.core.entities.Player;
import ludo.core.events.*;
import java.util.List;
import java.util.Map;

public class GameStateManager implements MessageListener {
    private final ClientNetworkHandler networkHandler;
    private GameScreen gameScreen;
    private boolean isMyTurn;
    private int currentDiceValue;

    public GameStateManager() {
        this.networkHandler = new ClientNetworkHandler();
        this.networkHandler.setMessageListener(this);
    }

    public void initialize(GameScreen gameScreen) {
        this.gameScreen = gameScreen;
        this.networkHandler.connect("localhost", 12000);
    }

    @Override
    public void onMessageReceived(NetworkMessage message) {
        switch (message.getType()) {
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
        }
    }

    @Override
    public void onConnectionError(Exception error) {
        if (gameScreen != null) {
            gameScreen.showMessage("Connection error: " + error.getMessage());
        }
    }


//    public void requestDiceRoll() {
//        networkHandler.sendMessage(new DiceRollRequestEvent());
//    }
//
//    public void requestMove(int pawnIndex) {
//        networkHandler.sendMessage(new MoveRequestEvent(pawnIndex));
//    }

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

//    public void updateGameState(Map<String, List<Integer>> pawnPositions, String currentPlayer, GameState gameState) {
//    }
//
//    public void handleDiceRoll(int value, String playerColor) {
//    }

//    public void updatePawnPosition(int pawnIndex, int newPosition) {
//    }
//
//    public void showErrorMessage(String message) {
//    }
//
//    public void startGame(List<Player> players, String startingPlayer) {
//    }
//
//    public void handleDisconnection() {
//    }
//
//    public void handleReconnection() {
//    }
//
//    public void sendPong() {
//    }
//
//    public void addPlayer(Player player) {
//    }
//
//    public void removePlayer(String playerName) {
//    }

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
