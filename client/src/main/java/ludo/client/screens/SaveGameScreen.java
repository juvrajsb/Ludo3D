package ludo.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import ludo.client.LudoGame;
import ludo.core.persistence.GamePersistence;
import ludo.core.persistence.GamePersistence.GameSaveData;
import ludo.core.game.GameState;
import ludo.core.entities.Player;
import ludo.core.entities.Pawn;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

public class SaveGameScreen extends BaseScreen {
    private static final Logger LOGGER = Logger.getLogger(SaveGameScreen.class.getName());
    private final Table mainTable;
    private final Label statusLabel;
    private final Label autoSaveLabel;

    public SaveGameScreen(final LudoGame game) {
        super(game);

        mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.defaults().pad(10).width(200);

        // Title
        Label titleLabel = new Label("Save Game", skin);
        titleLabel.setAlignment(Align.center);
        mainTable.add(titleLabel).colspan(2).pad(30).row();

        // Status labels
        Table statusTable = new Table();
        statusTable.defaults().pad(5);
        
        statusLabel = new Label("", skin);
        statusLabel.setWrap(true);
        statusTable.add(statusLabel).width(300).row();
        
        autoSaveLabel = new Label("", skin);
        autoSaveLabel.setWrap(true);
        statusTable.add(autoSaveLabel).width(300).row();
        
        mainTable.add(statusTable).colspan(2).pad(20).row();

        // Create buttons table
        Table buttonsTable = new Table();
        buttonsTable.defaults().pad(5).width(150);

        // Save button
        TextButton saveButton = new TextButton("Save Game", skin);
        saveButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                saveGame();
            }
        });

        // Load button
        TextButton loadButton = new TextButton("Load Game", skin);
        loadButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                loadGame(false);
            }
        });

        // Load Auto-save button
        TextButton loadAutoSaveButton = new TextButton("Load Auto-save", skin);
        loadAutoSaveButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                loadGame(true);
            }
        });

        // Delete save button
        TextButton deleteButton = new TextButton("Delete Saves", skin);
        deleteButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                deleteSaves();
            }
        });

        // Back button
        TextButton backButton = new TextButton("Back", skin);
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new GameScreen(game));
            }
        });

        // Add buttons to table
        buttonsTable.add(saveButton).row();
        buttonsTable.add(loadButton).row();
        buttonsTable.add(loadAutoSaveButton).row();
        buttonsTable.add(deleteButton).row();
        buttonsTable.add(backButton).row();

        mainTable.add(buttonsTable).colspan(2).pad(10).row();

        stage.addActor(mainTable);

        updateSaveStatus();
    }

    private void saveGame() {
        try {
            GamePersistence.saveGame(
                game.getPlayers(),
                game.getGameStateManager().getCurrentColor(),
                game.getGameStateManager().isGameStarted() ? GameState.IN_PROGRESS : GameState.WAITING_FOR_PLAYERS
            );
            updateSaveStatus();
            statusLabel.setText("[GREEN]Game saved successfully![]");
        } catch (Exception e) {
            LOGGER.severe("Failed to save game: " + e.getMessage());
            statusLabel.setText("[RED]Failed to save game: " + e.getMessage() + "[]");
        }
    }

    private void loadGame(boolean loadAutoSave) {
        try {
            GameSaveData saveData = loadAutoSave ? GamePersistence.loadAutoSave() : GamePersistence.loadGame();
            if (saveData != null) {
                // Clear current game state
                game.reset();

                // Load players and pawns
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
                });

                // Set current player
                game.getGameStateManager().setCurrentPlayer(saveData.currentPlayerColor);

                // Switch to game screen
                game.setScreen(new GameScreen(game));
                LOGGER.info("Game loaded successfully");
            } else {
                String source = loadAutoSave ? "auto-save" : "save";
                statusLabel.setText("[RED]No valid " + source + " found.[]");
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to load game: " + e.getMessage());
            statusLabel.setText("[RED]Failed to load game: " + e.getMessage() + "[]");
        }
    }

    private void deleteSaves() {
        GamePersistence.deleteSaveGame();
        GamePersistence.deleteAutoSave();
        updateSaveStatus();
        statusLabel.setText("[GREEN]All save files deleted.[]");
    }

    private void updateSaveStatus() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

        // Update manual save status
        if (GamePersistence.hasSaveGame()) {
            GameSaveData saveData = GamePersistence.loadGame();
            if (saveData != null) {
                String saveInfo = String.format("[WHITE]Manual save:\n%d players\nLast saved: %s[]",
                    saveData.players.size(),
                    dateFormat.format(new Date(saveData.timestamp)));
                statusLabel.setText(saveInfo);
            }
        } else {
            statusLabel.setText("[GRAY]No manual save found.[]");
        }

        // Update auto-save status
        if (GamePersistence.hasAutoSave()) {
            GameSaveData autoSaveData = GamePersistence.loadAutoSave();
            if (autoSaveData != null) {
                String autoSaveInfo = String.format("[WHITE]Auto-save:\n%d players\nLast saved: %s[]",
                    autoSaveData.players.size(),
                    dateFormat.format(new Date(autoSaveData.timestamp)));
                autoSaveLabel.setText(autoSaveInfo);
            }
        } else {
            autoSaveLabel.setText("[GRAY]No auto-save found.[]");
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }
}
