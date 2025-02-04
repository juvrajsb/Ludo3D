package ludo.client;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import ludo.client.screens.*;
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
        gameStateManager = new GameStateManager(this);
        // Start with connection screen instead of menu
//        setScreen(new ConnectionScreen(this));
        setScreen(new MenuScreen(this));
    }

    @Override
    public void setScreen(Screen screen) {
        if (this.screen != null) {
            this.screen.dispose();
        }
        super.setScreen(screen);
        if (screen instanceof GameScreen) {
            gameStateManager.initialize((GameScreen) screen);
        } else if (screen instanceof LobbyScreen) {
            gameStateManager.setLobbyScreen((LobbyScreen) screen);
        }
    }

    public GameStateManager getGameStateManager() {
        return gameStateManager;
    }

    public void startGame(String playerName, String color) {
        this.currentPlayerName = playerName;

        // Create initial player
        Player localPlayer = new Player(playerName, color);
        players.clear(); // Clear any existing players
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

    public void reset() {
        players.clear();
        currentPlayerName = null;
        if (gameScreen != null) {
            gameScreen.dispose();
            gameScreen = null;
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (gameStateManager != null) {
            gameStateManager.dispose();
        }
        reset();
    }
}
