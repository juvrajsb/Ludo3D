package ludo.client;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import ludo.client.screens.*;

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
