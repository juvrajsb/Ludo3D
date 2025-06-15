package ludo.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import ludo.client.LudoGame;
import ludo.core.entities.BotPlayer;
import ludo.core.entities.Player;
import ludo.core.utils.Constants;

import java.io.File;
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
        mainTable.defaults().pad(10);

        Label titleLabel = new Label("Game Lobby", skin, "default");
        mainTable.add(titleLabel).colspan(2).pad(50).row();

        playersTable = new Table(skin);
        playersTable.defaults().pad(5);

        Label playersLabel = new Label("Players:", skin);
        mainTable.add(playersLabel).colspan(2).pad(20).row();

        ScrollPane scrollPane = new ScrollPane(playersTable, skin);
        mainTable.add(scrollPane).width(300).height(200).row();

        botPlayersCheckbox = new CheckBox(" Enable Bot Players", skin);
        botPlayersCheckbox.setVisible(false);
        botPlayersCheckbox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateStartButtonState();
            }
        });
        mainTable.add(botPlayersCheckbox).colspan(2).pad(10).row();

        loadGameButton = new TextButton("Load Game", skin);
        loadGameButton.setVisible(false);
        loadGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // This now asks the server for the list instead of trying to read it locally
                game.getGameStateManager().requestSaveFilesList();
            }
        });
        mainTable.add(loadGameButton).colspan(2).pad(10).row();

        statusLabel = new Label("Waiting for players...", skin);
        mainTable.add(statusLabel).colspan(2).pad(20).row();

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
        mainTable.add(startButton).colspan(2).pad(20).row();

        stage.addActor(mainTable);
        game.getGameStateManager().setLobbyScreen(this);
        checkAdminStatus();

        disconnectButton.setPosition(Gdx.graphics.getWidth() - 130, Gdx.graphics.getHeight() - 50);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        disconnectButton.setPosition(width - 130, height - 50);
    }

    public void showLoadGameDialog(List<String> fileNames) {
        Gdx.app.log("LobbyScreen", "Showing load game dialog with " + fileNames.size() + " files.");

        final com.badlogic.gdx.scenes.scene2d.ui.List<String> saveList = new com.badlogic.gdx.scenes.scene2d.ui.List<>(skin);
        if (!fileNames.isEmpty()) {
            Array<String> gdxFileNames = new Array<>(fileNames.toArray(new String[0]));
            saveList.setItems(gdxFileNames);
        }

        Dialog dialog = new Dialog("Select Save File", skin) {
            @Override
            protected void result(Object object) {
                if (Boolean.TRUE.equals(object)) {
                    String selectedFile = saveList.getSelected();
                    if (selectedFile != null) {
                        Gdx.app.log("LobbyScreen", "Load selected for file: " + selectedFile);
                        // Tell the server to start the game by loading this file
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
            dialog.getContentTable().add(saveList).width(400).height(200).pad(10).row();
            dialog.button("Load Selected", true);
        }
        dialog.button("Cancel", false);
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
            playersTable.add(nameLabel).padRight(20);
            playersTable.add(colorLabel).row();
        }
        updateStartButtonState();
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

        // This check ensures a smooth transition after the server starts the game.
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
