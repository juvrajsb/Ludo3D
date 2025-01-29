package ludo.client.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import ludo.client.LudoGame;

/**
 * This class is responsible for the menu screen.
 * It extends the BaseScreen class.
 */
public class MenuScreen extends BaseScreen {
    private Skin skin;
    private Table mainTable;

    public MenuScreen(LudoGame game) {
        super(game);

        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        createUI();
    }

    private void createUI() {
        mainTable = new Table();
        mainTable.setFillParent(true);

        TextButton playButton = new TextButton("Play", skin);
        TextButton exitButton = new TextButton("Exit", skin);

        mainTable.add(playButton).pad(10).row();
        mainTable.add(exitButton).pad(10);

        playButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new LobbyScreen(game));
            }
        });

        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });

        stage.addActor(mainTable);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        super.render(delta);
    }

    @Override
    public void dispose() {
        super.dispose();
        skin.dispose();
    }
}
