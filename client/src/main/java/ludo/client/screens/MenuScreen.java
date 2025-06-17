package ludo.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import ludo.client.LudoGame;

public class MenuScreen extends BaseScreen {
//    private Texture logoTexture;
//    private Image logoImage;

    public MenuScreen(final LudoGame game) {
        super(game);

//        logoTexture = new Texture(Gdx.files.internal("images/iconw.png"));
//        logoImage = new Image(logoTexture);

        createUI();
    }

    private void createUI() {
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.defaults().pad(10).width(200);

        Label.LabelStyle titleStyle = new Label.LabelStyle(skin.get("default", Label.LabelStyle.class));
        titleStyle.font = skin.getFont("default");
        titleStyle.fontColor = Color.WHITE;

        Label titleLabel = new Label("LUDO 3D", titleStyle);
        titleLabel.setFontScale(3.0f); // Make the text larger

        mainTable.add(titleLabel).pad(50).row();

        // Add logo at the top
//        mainTable.add(logoImage).width(400).height(200).padBottom(50).row();

        TextButton playButton = new TextButton("Play", skin);
        TextButton exitButton = new TextButton("Exit", skin);
        disconnectButton.remove();

        mainTable.add(playButton).row();
        mainTable.add(exitButton).row();

        playButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new ConnectionScreen(game));
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

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        super.dispose();
//        if (logoTexture != null) {
//            logoTexture.dispose();
//        }
    }
}
