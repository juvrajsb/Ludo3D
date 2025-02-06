package ludo.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import ludo.client.LudoGame;
import ludo.core.persistence.GamePersistence;
import ludo.core.persistence.GamePersistence.GameSaveData;
import ludo.core.game.GameState;
import ludo.core.entities.Player;
import ludo.core.entities.Pawn;

public class SaveGameScreen extends BaseScreen {
    private final Table mainTable;
    private final Label statusLabel;

    public SaveGameScreen(final LudoGame game) {
        super(game);

        mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.defaults().pad(10).width(200);

        // Title
        Label titleLabel = new Label("Save Game", skin);
        mainTable.add(titleLabel).colspan(2).pad(50).row();

        // Status label
        statusLabel = new Label("", skin);
        mainTable.add(statusLabel).colspan(2).pad(20).row();

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
                loadGame();
            }
        });

        // Delete save button
        TextButton deleteButton = new TextButton("Delete Save", skin);
        deleteButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                deleteSave();
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

        // Add buttons
        mainTable.add(saveButton).colspan(2).pad(10).row();
        mainTable.add(loadButton).colspan(2).pad(10).row();
        mainTable.add(deleteButton).colspan(2).pad(10).row();
        mainTable.add(backButton).colspan(2).pad(10).row();

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
            statusLabel.setText("Game saved successfully!");
            updateSaveStatus();
        } catch (Exception e) {
            statusLabel.setText("Failed to save game: " + e.getMessage());
        }
    }

    private void loadGame() {
        try {
            GameSaveData saveData = GamePersistence.loadGame();
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
                statusLabel.setText("Game loaded successfully!");
            } else {
                statusLabel.setText("No saved game found.");
            }
        } catch (Exception e) {
            statusLabel.setText("Failed to load game: " + e.getMessage());
        }
    }

    private void deleteSave() {
        GamePersistence.deleteSaveGame();
        updateSaveStatus();
        statusLabel.setText("Save file deleted.");
    }

    private void updateSaveStatus() {
        if (GamePersistence.hasSaveGame()) {
            GameSaveData saveData = GamePersistence.loadGame();
            if (saveData != null) {
                String saveInfo = String.format("Save found with %d players\nLast saved: %s",
                    saveData.players.size(),
                    new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
                        .format(new java.util.Date(saveData.timestamp)));
                statusLabel.setText(saveInfo);
            }
        } else {
            statusLabel.setText("No saved game found.");
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
