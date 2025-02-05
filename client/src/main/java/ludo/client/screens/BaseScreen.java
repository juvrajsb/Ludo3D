package ludo.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import ludo.client.LudoGame;

/**
 * This class is responsible for the base screen.
 * It is an abstract class that implements the Screen interface.
 */
public abstract class BaseScreen implements Screen {
    protected final LudoGame game;
    protected Stage stage;
    protected InputMultiplexer inputMultiplexer;
    protected Viewport viewport;
    protected Skin skin;

    public BaseScreen(LudoGame game) {
        this.game = game;
        this.viewport = new ScreenViewport();
        this.stage = new Stage(viewport);
        this.inputMultiplexer = new InputMultiplexer(stage);
        this.skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        stage.getViewport().update(width, height, true);
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
    }
}
