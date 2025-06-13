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
import ludo.core.persistence.GamePersistence;
import ludo.core.persistence.GamePersistence.GameSaveData;
import ludo.core.entities.Pawn;
import com.badlogic.gdx.utils.Array;

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

        // Load Game button (only visible for admin)
        loadGameButton = new TextButton("Load Game", skin);
        loadGameButton.setVisible(false);
        loadGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showNativeFileChooserAndLoad();
            }
        });
        mainTable.add(loadGameButton).colspan(2).pad(30);
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
                    game.getGameStateManager().startGame(botPlayersCheckbox.isChecked(), false);
                }
            }
        });
        mainTable.add(startButton).colspan(2).pad(20).row();

        stage.addActor(mainTable);
        game.getGameStateManager().setLobbyScreen(this);
        checkAdminStatus();

        // disconnect button in top-right corner
        disconnectButton.setPosition(Gdx.graphics.getWidth() - 130, Gdx.graphics.getHeight() - 50);

        System.out.println("Working directory: " + System.getProperty("user.dir"));
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        disconnectButton.setPosition(width - 130, height - 50);
    }

    private void showNativeFileChooserAndLoad() {
        Gdx.app.log("LobbyScreen", "showNativeFileChooserAndLoad (libGDX dialog) called");
        File savesDir = new File("LudoSaves");
        if (!savesDir.exists()) {
            savesDir.mkdirs();
        }

        Gdx.app.log("LobbyScreen", "Using saves directory: " + savesDir.getAbsolutePath());

        // List all .json files in the directory
        File[] saveFiles = savesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        Array<String> fileNames = new Array<>();
        if (saveFiles != null) {
            for (File f : saveFiles) {
                fileNames.add(f.getName());
            }
        }

        final com.badlogic.gdx.scenes.scene2d.ui.List<String> saveList = new com.badlogic.gdx.scenes.scene2d.ui.List<>(skin);
        if (fileNames.size > 0) {
            saveList.setItems(fileNames);
            saveList.setSelectedIndex(0);
        }

        Dialog dialog = new Dialog("Select Save File", skin) {
            @Override
            protected void result(Object object) {
                if ("AUTO_SAVE".equals(object)) {
                    Gdx.app.log("LobbyScreen", "Loading auto save");
                    loadSavedGame();
                } else if (Boolean.TRUE.equals(object)) {
                    String fileName = saveList.getSelected();
                    if (fileName != null) {
                        Gdx.app.log("LobbyScreen", "Selected file: " + fileName);
                        loadSavedGame();
                    } else {
                        Gdx.app.log("LobbyScreen", "No file selected");
                    }
                } else {
                    Gdx.app.log("LobbyScreen", "Dialog cancelled or no file selected");
                }
            }
        };
        dialog.getContentTable().pad(20);
        if (fileNames.size == 0) {
            dialog.text("No save files found in 'assets/LudoSaves'.");
        } else {
            dialog.getContentTable().add(saveList).width(300).height(200).expandX().fillX().row();
            dialog.button("Load", true);
        }
        dialog.button("Cancel", false);
        dialog.button("Load Auto Save", "AUTO_SAVE");
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
        boolean currentAdmin = game.getGameStateManager().isFirstPlayer();
        Gdx.app.log("LobbyScreen", "checkAdminStatus: currentAdmin=" + currentAdmin + ", lastAdminStatus=" + lastAdminStatus);
        if (currentAdmin != lastAdminStatus) {
            isAdmin = currentAdmin;
            startButton.setVisible(isAdmin);
            botPlayersCheckbox.setVisible(isAdmin);
            loadGameButton.setVisible(isAdmin);
            if (isAdmin) {
                startButton.setDisabled(true);
            }
            lastAdminStatus = currentAdmin;
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
        Gdx.app.log("LobbyScreen", "setFirstPlayer called");
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
        checkAdminStatus(); // Ensure UI is updated when FIRST_PLAYER event is received
    }

    private void loadSavedGame() {
        GameSaveData saveData = GamePersistence.loadGame();
        if (saveData != null) {
            game.reset();

            // Create new game screen first
            GameScreen newGameScreen = new GameScreen(game);
            
            // Add players to both game and game screen
            saveData.players.forEach(playerData -> {
                Player player = new Player(playerData.name, playerData.color);
                for (int i = 0; i < playerData.pawns.size(); i++) {
                    GamePersistence.PawnSaveData pawnData = playerData.pawns.get(i);
                    Pawn pawn = player.getPawns().get(i);
                    pawn.setPosition(pawnData.position);
                    if (pawnData.isHome) pawn.sendHome();
                    if (pawnData.isFinished) pawn.setFinished(true);
                }
                game.addPlayer(player);
                newGameScreen.addPlayer(player);  // Add player to GameScreen as well
            });

            // Set current player and switch to game screen
            newGameScreen.setCurrentPlayer(saveData.currentPlayerColor);
            game.setScreen(newGameScreen);
        } else {
            Gdx.app.error("LobbyScreen", "Failed to load saved game");
        }
    }
}
