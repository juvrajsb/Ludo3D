package ludo.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Timer;
import ludo.client.LudoGame;
import com.badlogic.gdx.graphics.GL20;

public class ConnectionScreen extends BaseScreen {
    private final TextField ipField;
    private final TextField portField;
    private final Label errorLabel;
    private final Label statusLabel;
    private final TextButton connectButton;
    private boolean isConnected = false;

    public ConnectionScreen(final LudoGame game) {
        super(game);

        disconnectButton.remove();

        Table mainTable = new Table(skin);
        mainTable.setFillParent(true);

        // Title
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

        // Status label
        statusLabel = new Label("", skin);
        statusLabel.setColor(Color.WHITE);
        mainTable.add(statusLabel).colspan(2).padTop(20).row();

        // Error label
        errorLabel = new Label("", skin);
        errorLabel.setColor(Color.RED);
        mainTable.add(errorLabel).colspan(2).padTop(20).row();

        // Connect button
        connectButton = new TextButton("Connect", skin);
        connectButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                tryConnect();
            }
        });
        mainTable.add(connectButton).colspan(2).padTop(40).width(150).height(50).row();

        // Back button
        TextButton backButton = new TextButton("Back", skin);
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new MenuScreen(game));
            }
        });
        mainTable.add(backButton).colspan(2).padTop(20).width(150).height(50).row();

        stage.addActor(mainTable);
    }

    private void tryConnect() {
        errorLabel.setText("");
        statusLabel.setText("Connecting...");
        connectButton.setDisabled(true);

        String ip = ipField.getText().trim();
        try {
            int port = Integer.parseInt(portField.getText().trim());

            // Run connection attempt in separate thread to avoid blocking UI
            new Thread(() -> {
                try {
                    final boolean success = game.getGameStateManager().connect(ip, port);
                    // Update UI on main thread
                    Gdx.app.postRunnable(() -> {
                        if (success) {
                            isConnected = true;
                            statusLabel.setText("Connected!");
                            statusLabel.setColor(Color.GREEN);
                            // Move to username screen after short delay
                            Timer.schedule(new Timer.Task() {
                                @Override
                                public void run() {
                                    game.setScreen(new UsernameScreen(game));
                                }
                            }, 1);
                        } else {
                            connectButton.setDisabled(false);
                            errorLabel.setText("Failed to connect to server");
                            statusLabel.setText("");
                        }
                    });
                } catch (Exception e) {
                    Gdx.app.postRunnable(() -> {
                        connectButton.setDisabled(false);
                        errorLabel.setText("Connection error: " + e.getMessage());
                        statusLabel.setText("");
                    });
                }
            }, "Connection-Thread").start();

        } catch (NumberFormatException e) {
            connectButton.setDisabled(false);
            errorLabel.setText("Invalid port number");
            statusLabel.setText("");
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

//    public boolean isConnected() {//TODO check usage not used currently
//        return isConnected;
//    }
}
