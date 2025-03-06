package ludo.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
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

    public LobbyScreen(final LudoGame game) {
        super(game);

        Table mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.defaults().pad(10);

        // Title
        Label titleLabel = new Label("Game Lobby", skin, "default");
        mainTable.add(titleLabel).colspan(2).pad(50);
        mainTable.row();

        // Players list section
        playersTable = new Table(skin);
        playersTable.defaults().pad(5);

        Label playersLabel = new Label("Players:", skin);
        mainTable.add(playersLabel).colspan(2).pad(20);
        mainTable.row();

        ScrollPane scrollPane = new ScrollPane(playersTable, skin);
        mainTable.add(scrollPane).width(300).height(200);
        mainTable.row();

        // Bot players checkbox (only visible for admin)
        botPlayersCheckbox = new CheckBox(" Enable Bot Players", skin);
        botPlayersCheckbox.setVisible(false);
        botPlayersCheckbox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                handleBotPlayersToggle(botPlayersCheckbox.isChecked());
            }
        });
        mainTable.add(botPlayersCheckbox).colspan(2).pad(10);
        mainTable.row();

        // Status label
        statusLabel = new Label("Waiting for players...", skin);
        mainTable.add(statusLabel).colspan(2).pad(20);
        mainTable.row();

        // Start button
        startButton = new TextButton("Start Game", skin);
        startButton.setVisible(false);
        startButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (isAdmin && !startButton.isDisabled()) {
                    checkForSavedGame();
                }
            }
        });
        mainTable.add(startButton).colspan(2).pad(20).row();

        // Back button
        TextButton backButton = new TextButton("Leave", skin);
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.getGameStateManager().leaveGame();
                game.setScreen(new ConnectionScreen(game));
            }
        });
        mainTable.add(backButton).colspan(2).pad(20);

        stage.addActor(mainTable);
        game.getGameStateManager().setLobbyScreen(this);
        checkAdminStatus();
    }

    private void checkForSavedGame() {
        if (game.getGameStateManager().hasSavedGameWithMatchingPlayers()) {
            showLoadSavedGameDialog();
        } else {
            // No saved game or no matching players
            game.getGameStateManager().startGame(botPlayersCheckbox.isChecked(), false);
        }
    }

    private void showLoadSavedGameDialog() {
        Dialog dialog = new Dialog("Load Saved Game", skin) {
            @Override
            protected void result(Object object) {
                boolean loadSavedGame = (Boolean) object;
                game.getGameStateManager().startGame(botPlayersCheckbox.isChecked(), loadSavedGame);
            }
        };
        dialog.text("A saved game with matching players was found.\nWould you like to load it?");
        dialog.button("Yes", true);
        dialog.button("No", false);
        dialog.show(stage);
    }

    private void handleBotPlayersToggle(boolean enableBots) {
        if (enableBots) {
            // Only enable if we have room for bots
            int currentPlayers = players.size();
            if (currentPlayers < Constants.MAX_PLAYERS) {
                statusLabel.setText("Bot players will fill remaining slots");
                updateStartButtonState();
            }
        } else {
            updatePlayersList(players);
        }
    }

    private void checkAdminStatus() {
        isAdmin = game.getGameStateManager().isFirstPlayer();
        startButton.setVisible(isAdmin);
        botPlayersCheckbox.setVisible(isAdmin);
        if (isAdmin) {
            startButton.setDisabled(true);
        }
    }

    public void addPlayer(Player player) {
        players.add(player);
        updatePlayerList();
    }

    private void updatePlayerList() {
        playersTable.clear();
        for (Player player : players) {
            Label nameLabel = new Label(player.getName(), skin);
            Label colorLabel = new Label(player.getColor() + (player instanceof BotPlayer ? " (Bot)" : ""), skin);
            playersTable.add(nameLabel).padRight(20);
            playersTable.add(colorLabel);
            playersTable.row();
        }
    }

    public void updatePlayersList(List<Player> updatedPlayers) {
        players.clear();
        players.addAll(updatedPlayers);

        playersTable.clear();
        for (Player player : players) {
            Label nameLabel = new Label(player.getName(), skin);
            Label colorLabel = new Label(player.getColor() + (player instanceof BotPlayer ? " (Bot)" : ""), skin);
            playersTable.add(nameLabel).padRight(20);
            playersTable.add(colorLabel);
            playersTable.row();
        }

        int playerCount = players.size();
        if (playerCount < 2) {
            statusLabel.setText("Waiting for more players... (" + playerCount + "/4)");
            if (isAdmin) {
                startButton.setDisabled(!botPlayersCheckbox.isChecked());
            }
        } else {
            statusLabel.setText("Ready to start! (" + playerCount + "/4)");
            if (isAdmin) {
                startButton.setDisabled(false);
            }
        }

        updateStartButtonState();
    }

    public void updateStartButtonState() {
        if (isAdmin) {
            boolean enoughPlayers = players.size() >= 2 || botPlayersCheckbox.isChecked();
            startButton.setVisible(true);
            startButton.setDisabled(!enoughPlayers);
            startButton.setTouchable(enoughPlayers ? Touchable.enabled : Touchable.disabled);
        } else {
            startButton.setVisible(false);
            startButton.setTouchable(Touchable.disabled);
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        super.render(delta);

        if (game.getGameStateManager().isGameStarted()) {
            game.setScreen(new GameScreen(game));
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        game.getGameStateManager().setLobbyScreen(null);
    }

    public void showError(String message) {
        if (statusLabel != null) {
            statusLabel.setText("[RED]" + message + "[]");
        }
    }

    public void setFirstPlayer() {
        isAdmin = true;
        startButton.setVisible(true);
        botPlayersCheckbox.setVisible(true);
        startButton.setDisabled(true);

        int playerCount = players.size();
        if (playerCount >= 2 || botPlayersCheckbox.isChecked()) {
            statusLabel.setText("Ready to start! (" + playerCount + "/4)");
            startButton.setDisabled(false);
        } else {
            statusLabel.setText("Waiting for more players... (" + playerCount + "/4)");
        }
    }
}
