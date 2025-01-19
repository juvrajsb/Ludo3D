package ludo.client;

import com.badlogic.gdx.Game;
import ludo.client.screens.GameScreen;
import ludo.client.screens.MenuScreen;
import ludo.client.GameStateManager;
import ludo.core.entities.Player;

import java.util.ArrayList;
import java.util.List;

public class LudoGame extends Game {
    private GameStateManager gameStateManager;
    private List<Player> players;
    private String currentPlayerName;
    private GameScreen gameScreen;

    public LudoGame() {
        players = new ArrayList<>();
    }

    @Override
    public void create() {
        // Initialize game state manager
        gameStateManager = new GameStateManager();

        // Start with menu screen
        setScreen(new MenuScreen(this));
    }

    public GameStateManager getGameStateManager() {
        return gameStateManager;
    }

    public void startGame(String playerName, String color) {
        this.currentPlayerName = playerName;
        // Create initial player
        Player localPlayer = new Player(playerName, color);
        players.add(localPlayer);

        // Switch to game screen
        gameScreen = new GameScreen(this);
        setScreen(gameScreen);
    }

    public void addPlayer(Player player) {
        if (players.size() < 4 && !players.stream().anyMatch(p ->
            p.getColor().equals(player.getColor()) ||
                p.getName().equals(player.getName()))) {
            players.add(player);
        }
    }

    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    public String getCurrentPlayerName() {
        return currentPlayerName;
    }

    public Player getLocalPlayer() {
        return players.stream()
            .filter(p -> p.getName().equals(currentPlayerName))
            .findFirst()
            .orElse(null);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (gameStateManager != null) {
            // Clean up resources
        }
    }
}
