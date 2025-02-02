package ludo.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import ludo.client.LudoGame;
import ludo.core.entities.Player;

import java.util.ArrayList;
import java.util.List;

public class LobbyScreen extends BaseScreen {
    private final Table playersTable;
    private final Label statusLabel;
    private final TextButton startButton;
    private boolean isAdmin = false;
    private final List<Player> players = new ArrayList<>();

    public LobbyScreen(final LudoGame game) {
        super(game);
//        game.getGameStateManager().setLobbyScreen(this);

        Table mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.defaults().pad(10);

        // Title
        Label titleLabel = new Label("Game Lobby", skin, "default");
        mainTable.add(titleLabel).colspan(2).pad(50);
        mainTable.row();

        playersTable = new Table(skin);
        playersTable.defaults().pad(5);

        // Players list
        Label playersLabel = new Label("Players:", skin);
        mainTable.add(playersLabel).colspan(2).pad(20);
        mainTable.row();

        ScrollPane scrollPane = new ScrollPane(playersTable, skin);
        mainTable.add(scrollPane).width(300).height(200);
        mainTable.row();

        // Status label
        statusLabel = new Label("Waiting for players...", skin);
        mainTable.add(statusLabel).colspan(2).pad(20);
        mainTable.row();

        // Start button
        startButton = new TextButton("Start Game", skin);
        startButton.setVisible(false); // Hidden by default
        startButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (isAdmin && !startButton.isDisabled()) {
                    game.getGameStateManager().startGame();
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
        Gdx.input.setInputProcessor(stage);
        game.getGameStateManager().setLobbyScreen(this);

        // Check if this player is the admin (first player)
        checkAdminStatus();
    }

    private void checkAdminStatus() {
        // First player to join becomes admin
        isAdmin = game.getGameStateManager().isFirstPlayer();
        startButton.setVisible(isAdmin);
        if (isAdmin) {
            startButton.setDisabled(true); // Initially disabled until enough players
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
            Label colorLabel = new Label(player.getColor(), skin);
            playersTable.add(nameLabel).padRight(20);
            playersTable.add(colorLabel);
            playersTable.row();
        }
    }
    public void updatePlayersList(List<Player> updatedPlayers) {
        // Clear and update players list
        players.clear();
        players.addAll(updatedPlayers);

        // Update table display
        playersTable.clear();
        for (Player player : players) {
            Label nameLabel = new Label(player.getName(), skin);
            Label colorLabel = new Label(player.getColor(), skin);
            playersTable.add(nameLabel).padRight(20);
            playersTable.add(colorLabel);
            playersTable.row();
        }

        // Update status with correct player count
        int playerCount = players.size();  // Use the actual size of our players list
        if (playerCount < 2) {
            statusLabel.setText("Waiting for more players... (" + playerCount + "/4)");
            if (isAdmin) {
                startButton.setDisabled(true);
            }
        } else {
            statusLabel.setText("Ready to start! (" + playerCount + "/4)");
            if (isAdmin) {
                startButton.setDisabled(false);
            }
        }

        // Make sure start button state is updated
        updateStartButtonState();
    }

    public void updateStartButtonState() {
        if (isAdmin) {
            boolean enoughPlayers = players.size() >= 2;
            startButton.setVisible(true);
            startButton.setDisabled(!enoughPlayers);
            startButton.setTouchable(enoughPlayers ? Touchable.enabled : Touchable.disabled);
        } else {
            startButton.setVisible(false);
            startButton.setTouchable(Touchable.disabled);
        }
    }

//        // Update status
//        int playerCount = players.size();
//        if (playerCount < 2) {
//            statusLabel.setText("Waiting for more players... (" + playerCount + "/4)");
//            startButton.setDisabled(true);
//        } else {
//            statusLabel.setText("Ready to start! (" + playerCount + "/4)");
//            startButton.setDisabled(false);
//        }


    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

//        checkAdminStatus();
        super.render(delta);

        // Check for game start
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
        startButton.setDisabled(true); // Initially disabled
        // Also update status
        int playerCount = players.size();
        if (playerCount >= 2) {
            statusLabel.setText("Ready to start! (" + playerCount + "/4)");
            startButton.setDisabled(false);
        } else {
            statusLabel.setText("Waiting for more players... (" + playerCount + "/4)");
        }
    }
}
