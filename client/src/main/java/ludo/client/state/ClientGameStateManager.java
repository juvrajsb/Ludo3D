package ludo.client.state;

import ludo.core.entities.Player;
import ludo.core.game.GameState;
import ludo.core.network.NetworkHandler;
import ludo.core.events.clientToServer.*;
import java.util.*;

/**
 * This class is responsible for managing the game state.
 * It is a singleton class.
 */
public class ClientGameStateManager {
    private final NetworkHandler networkHandler;
    private final Map<String, Player> players;
    private String localPlayerId;
    private String currentPlayerId;
    private GameState gameState;
    private int lastDiceRoll;

    public ClientGameStateManager(NetworkHandler networkHandler) {
        this.networkHandler = networkHandler;
        this.players = new HashMap<>();
        this.gameState = GameState.WAITING_FOR_PLAYERS;
    }

    // Game actions
    public void requestDiceRoll() {
        if (isLocalPlayerTurn()) {
            networkHandler.sendMessage(new DiceRollRequestEvent());
        }
    }

    public void requestMove(int pawnIndex) {
        if (isLocalPlayerTurn() && lastDiceRoll > 0) {
            networkHandler.sendMessage(new MoveRequestEvent(pawnIndex, lastDiceRoll));
            lastDiceRoll = 0;
        }
    }

    public void joinGame(String playerName, String desiredColor) {
        localPlayerId = playerName;
        networkHandler.sendMessage(new JoinGameRequestEvent(playerName, desiredColor));
    }

    public void leaveGame() {
        networkHandler.sendMessage(new LeaveGameRequestEvent());
    }

    // State updates
    public void updateGameState(Map<String, List<Integer>> pawnPositions, String currentPlayer) {
        // Update pawn positions for all players
        pawnPositions.forEach((color, positions) -> {
            Player player = players.get(color);
            if (player != null) {
                for (int i = 0; i < positions.size(); i++) {
                    player.setPawnPosition(i, positions.get(i));
                }
            }
        });

        this.currentPlayerId = currentPlayer;
    }

    public void handleDiceRoll(int value) {
        this.lastDiceRoll = value;
    }

    public void addPlayer(Player player) {
        players.put(player.getColor(), player);
    }

    public void removePlayer(String color) {
        players.remove(color);
    }

    // State queries
    public boolean isLocalPlayerTurn() {
        return currentPlayerId != null && currentPlayerId.equals(localPlayerId);
    }

    public Player getLocalPlayer() {
        return players.values().stream()
            .filter(p -> p.getName().equals(localPlayerId))
            .findFirst()
            .orElse(null);
    }

    public Collection<Player> getPlayers() {
        return Collections.unmodifiableCollection(players.values());
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public int getLastDiceRoll() {
        return lastDiceRoll;
    }

    public String getCurrentPlayerId() {
        return currentPlayerId;
    }
}
