package ludo.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import ludo.client.LudoGame;
import ludo.core.entities.BotPlayer;
import ludo.core.entities.Player;
import ludo.core.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class LobbyScreen extends BaseScreen {
    private final Table playersTable;
    private final Label statusLabel;
    private final TextButton startButton;
    private final List<Player> players = new ArrayList<>();
    private boolean isAdmin = false;
    private CheckBox botPlayersCheckbox;
    private TextButton loadGameButton;
    private boolean lastAdminStatus = false;

    public LobbyScreen(final LudoGame game) {
        super(game);

        Table mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.pad(20);

        // Title
        Label titleLabel = new Label("Game Lobby", skin, "default");
        mainTable.add(titleLabel).colspan(2).expandX().padBottom(20).row();

        // Players List
        playersTable = new Table(skin);
        playersTable.defaults().pad(10).align(Align.left);
        ScrollPane scrollPane = new ScrollPane(playersTable, skin);
        scrollPane.setFadeScrollBars(false);

        Container<ScrollPane> playersContainer = new Container<>(scrollPane);
        playersContainer.setBackground(skin.newDrawable("white", new Color(0, 0, 0, 0.2f)));
        playersContainer.pad(10);
        mainTable.add(playersContainer).width(400).height(200).colspan(2).padBottom(20).row();


        // Admin Controls
        Table adminControls = new Table();
        adminControls.defaults().pad(5);
        botPlayersCheckbox = new CheckBox(" Enable Bot Players", skin);
        botPlayersCheckbox.setVisible(false);
        botPlayersCheckbox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateStartButtonState();
            }
        });

        loadGameButton = new TextButton("Load Game", skin);
        loadGameButton.setVisible(false);
        loadGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.getGameStateManager().requestSaveFilesList();
            }
        });

        adminControls.add(botPlayersCheckbox).left();
        adminControls.add(loadGameButton).width(150).height(40).padLeft(20);
        mainTable.add(adminControls).colspan(2).padBottom(10).row();

        // Status Label
        statusLabel = new Label("Waiting for players...", skin);
        mainTable.add(statusLabel).colspan(2).padBottom(20).row();

        // Start Button
        startButton = new TextButton("Start Game", skin);
        startButton.setVisible(false);
        startButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (isAdmin && !startButton.isDisabled()) {
                    game.getGameStateManager().startGame(botPlayersCheckbox.isChecked(), false);
                }
            }
        });
        mainTable.add(startButton).width(200).height(50).colspan(2);

        stage.addActor(mainTable);
        game.getGameStateManager().setLobbyScreen(this);
        checkAdminStatus();

        disconnectButton.setPosition(Gdx.graphics.getWidth() - 160, Gdx.graphics.getHeight() - 60);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        disconnectButton.setPosition(width - 160, height - 60);
    }

    public void showLoadGameDialog(List<String> fileNames) {
        Gdx.app.log("LobbyScreen", "Showing load game dialog with " + fileNames.size() + " files.");

        final com.badlogic.gdx.scenes.scene2d.ui.List<String> saveList = new com.badlogic.gdx.scenes.scene2d.ui.List<>(skin);
        if (!fileNames.isEmpty()) {
            Array<String> gdxFileNames = new Array<>(fileNames.toArray(new String[0]));
            saveList.setItems(gdxFileNames);
        }

        Dialog dialog = new Dialog("Select Save File", skin, "dialog") {
            @Override
            protected void result(Object object) {
                if (Boolean.TRUE.equals(object)) {
                    String selectedFile = saveList.getSelected();
                    if (selectedFile != null) {
                        Gdx.app.log("LobbyScreen", "Load selected for file: " + selectedFile);
                        game.getGameStateManager().startGame(botPlayersCheckbox.isChecked(), true, selectedFile);
                    }
                } else {
                    Gdx.app.log("LobbyScreen", "Load game dialog cancelled.");
                }
            }
        };

        dialog.getContentTable().pad(20);
        if (fileNames.isEmpty()) {
            dialog.text("No save files found on the server.");
        } else {
            dialog.getContentTable().add(new Label("Select a save to load:", skin)).row();
            ScrollPane scroll = new ScrollPane(saveList, skin);
            dialog.getContentTable().add(scroll).width(400).height(200).pad(10).row();

            TextButton loadButton = new TextButton("Load Selected", skin);
            dialog.button(loadButton, true);
        }

        TextButton cancelButton = new TextButton("Cancel", skin);
        dialog.button(cancelButton, false);

        dialog.getButtonTable().getCells().get(0).width(150).height(40).pad(10);
        if (!fileNames.isEmpty()) {
            dialog.getButtonTable().getCells().get(1).width(150).height(40).pad(10);
        }

        dialog.show(stage);
    }

    private void checkAdminStatus() {
        boolean currentAdmin = game.getGameStateManager().isFirstPlayer();
        if (currentAdmin != lastAdminStatus) {
            isAdmin = currentAdmin;
            startButton.setVisible(isAdmin);
            botPlayersCheckbox.setVisible(isAdmin);
            loadGameButton.setVisible(isAdmin);
            lastAdminStatus = currentAdmin;
            updateStartButtonState();
        }
    }

    public void updatePlayersList(List<Player> updatedPlayers) {
        this.players.clear();
        this.players.addAll(updatedPlayers);

        playersTable.clear();
        for (Player player : this.players) {
            Label nameLabel = new Label(player.getName(), skin);
            Label colorLabel = new Label(player.getColor() + (player instanceof BotPlayer ? " (Bot)" : ""), skin);
            colorLabel.setColor(getColorForName(player.getColor()));
            playersTable.add(nameLabel).padRight(20);
            playersTable.add(colorLabel).row();
        }
        updateStartButtonState();
    }

    private Color getColorForName(String colorName) {
        if (colorName == null) return Color.WHITE;
        switch(colorName.toUpperCase()) {
            case "RED": return Color.RED;
            case "BLUE": return Color.SKY;
            case "GREEN": return Color.LIME;
            case "YELLOW": return Color.YELLOW;
            default: return Color.WHITE;
        }
    }

    public void updateStartButtonState() {
        if (isAdmin) {
            int playerCount = players.size();
            boolean enoughPlayers = playerCount >= 2 || (playerCount >= 1 && botPlayersCheckbox.isChecked());

            if (enoughPlayers) {
                statusLabel.setText("Ready to start! (" + playerCount + "/" + Constants.MAX_PLAYERS + ")");
            } else {
                statusLabel.setText("Waiting for more players... (" + playerCount + "/" + Constants.MAX_PLAYERS + ")");
            }

            startButton.setDisabled(!enoughPlayers);
            startButton.setTouchable(enoughPlayers ? Touchable.enabled : Touchable.disabled);
        } else {
            int playerCount = players.size();
            statusLabel.setText("Waiting for admin to start... (" + playerCount + "/" + Constants.MAX_PLAYERS + ")");
            startButton.setDisabled(true);
        }
    }

    public void setFirstPlayer() {
        isAdmin = true;
        checkAdminStatus();
    }

    public void showError(String message) {
        if (statusLabel != null) {
            statusLabel.setText("[RED]" + message + "[]");
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (game.getGameStateManager().isGameStarted() && !(game.getScreen() instanceof GameScreen)) {
            // The GameStateManager will handle the actual screen transition.
        }

        super.render(delta);
    }

    @Override
    public void dispose() {
        super.dispose();
        game.getGameStateManager().setLobbyScreen(null);
    }
}
