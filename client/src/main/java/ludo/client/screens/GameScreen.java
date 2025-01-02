package ludo.client.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.Gdx;
import ludo.client.render.BoardRenderer;
import ludo.client.render.DiceRenderer;
import ludo.client.render.PawnRenderer;
import ludo.client.render.GameCamera;
import ludo.core.entities.Player;

public class GameScreen implements Screen {
    private final BoardRenderer boardRenderer;
    private final DiceRenderer diceRenderer;
    private final PawnRenderer pawnRenderer;
    private final GameCamera camera;
    private final Player[] players;

    public GameScreen() {
        this.boardRenderer = new BoardRenderer();
        this.diceRenderer = new DiceRenderer();
        this.pawnRenderer = new PawnRenderer();
        this.camera = new GameCamera();
        this.players = new Player[4]; // Initialize with 4 players

        // Initialize players with different colors
        players[0] = new Player("Player 1", "red");
        players[1] = new Player("Player 2", "blue");
        players[2] = new Player("Player 3", "green");
        players[3] = new Player("Player 4", "yellow");
    }

    @Override
    public void render(float delta) {
        // Clear screen
        Gdx.gl.glClearColor(0.8f, 0.8f, 0.8f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();

        // Render board
        boardRenderer.render();

        // Render dice (example value of 6)
        diceRenderer.render(6, 700, 100);

        // Render pawns for all players
        for (Player player : players) {
            pawnRenderer.render(player);
        }
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }

    @Override
    public void show() {
        // Called when this screen becomes the current screen
    }

    @Override
    public void hide() {
        // Called when this screen is no longer the current screen
    }

    @Override
    public void pause() {
        // Called when game is paused
    }

    @Override
    public void resume() {
        // Called when game is resumed
    }

    @Override
    public void dispose() {
        boardRenderer.dispose();
        diceRenderer.dispose();
        pawnRenderer.dispose();
    }
}

