package ludo.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
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
        game.getGameStateManager().setLobbyScreen(this);

        Table mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.defaults().pad(10);

        // Title
        Label titleLabel = new Label("Game Lobby", skin, "default");
        mainTable.add(titleLabel).colspan(2).pad(50);
        mainTable.row();

        // Players list
        Label playersLabel = new Label("Players:", skin);
        mainTable.add(playersLabel).colspan(2).pad(20);
        mainTable.row();

        playersTable = new Table(skin);
        playersTable.defaults().pad(5);
        ScrollPane scrollPane = new ScrollPane(playersTable, skin);
        mainTable.add(scrollPane).width(300).height(200);
        mainTable.row();

        // Status label
        statusLabel = new Label("Waiting for players...", skin);
        mainTable.add(statusLabel).colspan(2).pad(20);
        mainTable.row();

        // Start button (only visible for admin)
        startButton = new TextButton("Start Game", skin);
        startButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (isAdmin) {
                    game.getGameStateManager().startGame();
                }
            }
        });
        startButton.setVisible(false);
        mainTable.add(startButton).colspan(2).pad(20);

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

        // Check if this player is the admin (first player)
        checkAdminStatus();
    }

    private void checkAdminStatus() {
        // First player to join becomes admin
        isAdmin = game.getGameStateManager().isFirstPlayer();
        startButton.setVisible(isAdmin);
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
    public void updatePlayersList(List<Player> players) {
        playersTable.clear();
        for (Player player : players) {
            Label nameLabel = new Label(player.getName(), skin);
            Label colorLabel = new Label(player.getColor(), skin);
            playersTable.add(nameLabel).padRight(20);
            playersTable.add(colorLabel);
            playersTable.row();
        }

        // Update status
        int playerCount = players.size();
        if (playerCount < 2) {
            statusLabel.setText("Waiting for more players... (" + playerCount + "/4)");
            startButton.setDisabled(true);
        } else {
            statusLabel.setText("Ready to start! (" + playerCount + "/4)");
            startButton.setDisabled(false);
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

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

    public void setFirstPlayer() {
        isAdmin = true;
        startButton.setVisible(true);
    }
}
//public class LobbyScreen extends BaseScreen {
//    private final Table playersTable;
//    private final Label statusLabel;
//    private final TextButton startButton;
//    private SelectBox<Integer> playerCountSelect;
//    private boolean isAdmin = false;
//
//    public LobbyScreen(final LudoGame game) {
//        super(game);
//
//        Table mainTable = new Table();
//        mainTable.setFillParent(true);
//        mainTable.defaults().pad(10);
//
//        // Title
//        Label titleLabel = new Label("Game Lobby", skin);
//        mainTable.add(titleLabel).colspan(2).pad(50).row();
//
//        // Player count selection (only for first player)
//        playerCountSelect = new SelectBox<>(skin);
//        playerCountSelect.setItems(2, 3, 4);
//        playerCountSelect.setVisible(false);
//        mainTable.add(new Label("Number of Players:", skin));
//        mainTable.add(playerCountSelect).row();
//
//        // Players list
//        playersTable = new Table(skin);
//        ScrollPane scrollPane = new ScrollPane(playersTable, skin);
//        mainTable.add(scrollPane).width(300).height(200).colspan(2).row();
//
//        // Status label
//        statusLabel = new Label("Waiting for players...", skin);
//        mainTable.add(statusLabel).colspan(2).pad(20).row();
//
//        // Start button (only visible for admin)
//        startButton = new TextButton("Start Game", skin);
//        startButton.setVisible(false);
//        mainTable.add(startButton).colspan(2).width(150).padTop(40).row();
//
//        // Leave button
//        TextButton leaveButton = new TextButton("Leave", skin);
//        mainTable.add(leaveButton).colspan(2).width(150).padTop(20);
//
//        stage.addActor(mainTable);
//
//        // Button handlers
//        startButton.addListener(new ChangeListener() {
//            @Override
//            public void changed(ChangeEvent event, Actor actor) {
//                if (isAdmin) {
//                    game.getGameStateManager().startGame(playerCountSelect.getSelected());
//                }
//            }
//        });
//
//        leaveButton.addListener(new ChangeListener() {
//            @Override
//            public void changed(ChangeEvent event, Actor actor) {
//                game.getGameStateManager().leaveGame();
//                game.setScreen(new ConnectionScreen(game));
//            }
//        });
//    }
//
//    public void setFirstPlayer() {
//        isAdmin = true;
//        playerCountSelect.setVisible(true);
//        startButton.setVisible(true);
//    }
//}
