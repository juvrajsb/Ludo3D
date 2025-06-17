package ludo.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import ludo.client.LudoGame;

import java.util.logging.Logger;

public abstract class BaseScreen implements Screen {
    protected final LudoGame game;
    protected Stage stage;
    protected InputMultiplexer inputMultiplexer;
    protected Viewport viewport;
    protected Skin skin;
    protected TextButton disconnectButton;
    private static final Logger LOGGER = Logger.getLogger(BaseScreen.class.getName());

    public BaseScreen(LudoGame game) {
        this.game = game;
        this.viewport = new ScreenViewport();
        this.stage = new Stage(viewport);
        this.inputMultiplexer = new InputMultiplexer(stage);
        this.skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        disconnectButton = new TextButton("Disconnect", skin);
        disconnectButton.setSize(120, 40);
        disconnectButton.setPosition(Gdx.graphics.getWidth() - 130, 10);
        disconnectButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                handleDisconnect();
            }
        });
        stage.addActor(disconnectButton);
    }

    protected void handleDisconnect() { //todo check if correct
        if (game.getGameStateManager() != null) {
            LOGGER.info("Starting disconnection process...");
            game.getGameStateManager().leaveGame();
            game.setScreen(new ConnectionScreen(game));
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        stage.getViewport().update(width, height, true);
        disconnectButton.setPosition(width - 130, 10);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void render(float delta) {
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
