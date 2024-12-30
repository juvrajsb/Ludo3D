package ludo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class Lobby {
    private Stage stage;
    private Skin skin;
    private Label titleLabel;
    private TextButton startGameButton;
    private Client client;

    public Lobby(Client client) {
        this.client = client;
        stage = new Stage(new ScreenViewport());
        skin = SkinLoader.createSkin();
        createUI();
        Gdx.input.setInputProcessor(stage);
    }

    private void createUI() {
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        titleLabel = new Label("Ludo Game", skin);
        startGameButton = new TextButton("Start Game", skin);

        table.add(titleLabel).pad(10);
        table.row();
        table.add(startGameButton).pad(10);

        startGameButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                client.sendToServer("START_GAME");
            }
        });
    }

    public void updatePlayerList(String[] players) {
        startGameButton.setDisabled(players.length < 2);
        titleLabel.setText("Players: " + players.length);
    }

    public void render() {
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
