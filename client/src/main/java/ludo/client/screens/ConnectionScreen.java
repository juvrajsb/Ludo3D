package ludo.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Timer;
import ludo.client.LudoGame;
import ludo.client.ui.MenuUIHelper;

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

        Table rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.center();

        // Modern Card Container
        Table card = MenuUIHelper.createCard(skin, 24);
        card.defaults().align(Align.center).padBottom(10);

        // 1. 4-Color Accent Stripe
        card.add(MenuUIHelper.createColorStripe(skin, 4)).fillX().expandX().padBottom(16).row();

        // 2. Header
        Label.LabelStyle titleStyle = new Label.LabelStyle(skin.get("default", Label.LabelStyle.class));
        titleStyle.font = skin.getFont("window");
        titleStyle.fontColor = MenuUIHelper.TEXT_PRIMARY;

        Label titleLabel = new Label("Connect to Server", titleStyle);
        titleLabel.setFontScale(1.3f);
        titleLabel.setAlignment(Align.center);
        card.add(titleLabel).padBottom(4).row();

        Label subtitleLabel = new Label("Join a local match or connect over network", skin);
        subtitleLabel.setColor(MenuUIHelper.TEXT_MUTED);
        subtitleLabel.setAlignment(Align.center);
        card.add(subtitleLabel).padBottom(20).row();

        // 3. Form Table
        Table formTable = new Table();
        formTable.defaults().pad(6);

        Label ipLabel = new Label("Server IP:", skin);
        ipLabel.setColor(MenuUIHelper.TEXT_MUTED);
        formTable.add(ipLabel).right().padRight(12);

        ipField = new TextField("localhost", skin);
        formTable.add(ipField).width(200).height(36).left().row();

        Label portLabel = new Label("Port:", skin);
        portLabel.setColor(MenuUIHelper.TEXT_MUTED);
        formTable.add(portLabel).right().padRight(12);

        portField = new TextField("12000", skin);
        formTable.add(portField).width(200).height(36).left().row();

        card.add(formTable).padBottom(10).row();

        // 4. Quick Preset Button
        TextButton presetButton = new TextButton("⚡ Reset to Localhost (12000)", skin);
        presetButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ipField.setText("localhost");
                portField.setText("12000");
                errorLabel.setText("");
            }
        });
        card.add(presetButton).width(240).height(28).padBottom(16).row();

        // 5. Status & Error Feedback
        statusLabel = new Label("", skin);
        statusLabel.setColor(MenuUIHelper.SUCCESS_GREEN);
        statusLabel.setAlignment(Align.center);
        card.add(statusLabel).padBottom(6).row();

        errorLabel = new Label("", skin);
        errorLabel.setColor(MenuUIHelper.ERROR_RED);
        errorLabel.setAlignment(Align.center);
        errorLabel.setWrap(true);
        card.add(errorLabel).width(360).padBottom(16).row();

        // 6. Action Buttons
        connectButton = new TextButton("Connect to Server", skin);
        connectButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                tryConnect();
            }
        });
        card.add(connectButton).width(260).height(44).padBottom(10).row();

        TextButton backButton = new TextButton("← Back to Menu", skin);
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new MenuScreen(game));
            }
        });
        card.add(backButton).width(260).height(36).row();

        rootTable.add(card).width(440);
        stage.addActor(rootTable);
    }

    private void tryConnect() {
        errorLabel.setText("");
        statusLabel.setText("Connecting to server...");
        statusLabel.setColor(MenuUIHelper.ACCENT_GOLD);
        connectButton.setDisabled(true);

        String ip = ipField.getText().trim();
        try {
            int port = Integer.parseInt(portField.getText().trim());
            new Thread(() -> {
                try {
                    final boolean success = game.getGameStateManager().connect(ip, port);
                    Gdx.app.postRunnable(() -> {
                        if (success) {
                            isConnected = true;
                            statusLabel.setText("✓ Connected! Entering setup...");
                            statusLabel.setColor(MenuUIHelper.SUCCESS_GREEN);
                            Timer.schedule(new Timer.Task() {
                                @Override
                                public void run() {
                                    game.setScreen(new UsernameScreen(game));
                                }
                            }, 0.8f);
                        } else {
                            connectButton.setDisabled(false);
                            errorLabel.setText("Failed to connect to " + ip + ":" + port + "\nMake sure the game server is running.");
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
            errorLabel.setText("Please enter a valid numeric port (e.g. 12000)");
            statusLabel.setText("");
        }
    }
}
