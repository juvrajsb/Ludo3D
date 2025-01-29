package ludo.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import ludo.client.LudoGame;
import com.badlogic.gdx.graphics.GL20;

public class ConnectionScreen extends BaseScreen {
    private TextField ipField;
    private TextField portField;
    private Label errorLabel;

    public ConnectionScreen(final LudoGame game) {
        super(game);

        // Create main table
        Table mainTable = new Table(skin);
        mainTable.setFillParent(true);

        // Add title
        Label titleLabel = new Label("Connect to Server", skin, "default");
        mainTable.add(titleLabel).colspan(2).padBottom(50).row();

        // IP Address field
        mainTable.add(new Label("IP Address:", skin)).padRight(20);
        ipField = new TextField("localhost", skin);
        mainTable.add(ipField).width(200).row();

        // Port field
        mainTable.add(new Label("Port:", skin)).padRight(20).padTop(20);
        portField = new TextField("12000", skin);
        mainTable.add(portField).width(200).padTop(20).row();

        // Error label
        errorLabel = new Label("", skin);
        errorLabel.setColor(1, 0, 0, 1); // Red color
        mainTable.add(errorLabel).colspan(2).padTop(20).row();

        // Connect button
        TextButton connectButton = new TextButton("Connect", skin);
        connectButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                tryConnect();
            }
        });
        mainTable.add(connectButton).colspan(2).padTop(40).width(150).height(50).row();

        // Add the table to the stage
        stage.addActor(mainTable);

        // Set input processor
        Gdx.input.setInputProcessor(stage);
    }

    private void tryConnect() {
        String ip = ipField.getText().trim();
        try {
            int port = Integer.parseInt(portField.getText().trim());

            // Attempt connection
            if (game.getGameStateManager().connect(ip, port)) {
                // On successful connection, move to username screen
                game.setScreen(new UsernameScreen(game));
            } else {
                errorLabel.setText("Failed to connect to server");
            }
        } catch (NumberFormatException e) {
            errorLabel.setText("Invalid port number");
        }
    }

    @Override
    public void render(float delta) {
        // Clear the screen
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Update and draw stage
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
