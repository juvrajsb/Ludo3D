package ludo.client;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import ludo.client.screens.*;
import ludo.core.entities.Player;
import java.util.ArrayList;
import java.util.List;

/**
 * The main entry point for the LibGDX client application.
 * Its primary role is to manage screen transitions and hold the single instance of GameStateManager.
 */
public class LudoGame extends Game {
    private GameStateManager gameStateManager;
    private GameScreen gameScreen; // Keep a reference to dispose it correctly

    @Override
    public void create() {
        gameStateManager = new GameStateManager(this);
        setScreen(new MenuScreen(this));
    }

    @Override
    public void setScreen(Screen screen) {
        if (this.screen != null) {
            this.screen.dispose();
        }
        super.setScreen(screen);

        // Update GameStateManager's knowledge of the current screen
        if (screen instanceof GameScreen) {
            this.gameScreen = (GameScreen) screen;
            gameStateManager.setGameScreen((GameScreen) screen);
        } else if (screen instanceof LobbyScreen) {
            gameStateManager.setLobbyScreen((LobbyScreen) screen);
        }
    }

    /**
     * Returns the single instance of the GameStateManager.
     */
    public GameStateManager getGameStateManager() {
        return gameStateManager;
    }

    /**
     * Gets the current list of players directly from the GameStateManager.
     * The GameStateManager is the client's single source of truth for game state.
     * @return The current list of players.
     */
    public List<Player> getPlayers() {
        return gameStateManager.getCurrentPlayers();
    }

    /**
     * Resets the client-side state by telling the GameStateManager to reset.
     * This is called when returning to the main menu or disconnecting.
     */
    public void reset() {
        if (gameStateManager != null) {
            gameStateManager.resetGame();
        }
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
        if (screen != null) {
            screen.dispose();
        }
    }
}
