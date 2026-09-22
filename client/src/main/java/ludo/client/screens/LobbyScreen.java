package ludo.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import ludo.client.LudoGame;
import ludo.client.ui.MenuUIHelper;
import ludo.core.entities.BotPlayer;
import ludo.core.entities.Player;
import ludo.core.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class LobbyScreen extends BaseScreen {
    private final Table playersTable;
    private final Label statusLabel;
    private final Label playerCountLabel;
    private final TextButton startButton;
    private final List<Player> players = new ArrayList<>();
    private boolean isAdmin = false;
    private CheckBox botPlayersCheckbox;
    private TextButton loadGameButton;
    private boolean lastAdminStatus = false;

    public LobbyScreen(final LudoGame game) {
        super(game);

        Table rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.center();

        // Generously sized Card Container (width 580)
        Table card = MenuUIHelper.createCard(skin, 24);
        card.defaults().align(Align.center).padBottom(10);

        // 1. 4-Color Accent Stripe
        card.add(MenuUIHelper.createColorStripe(skin, 4)).fillX().expandX().padBottom(16).row();

        // 2. Header
        Label.LabelStyle titleStyle = new Label.LabelStyle(skin.get("default", Label.LabelStyle.class));
        titleStyle.font = skin.getFont("window");
        titleStyle.fontColor = MenuUIHelper.TEXT_PRIMARY;

        Label titleLabel = new Label("Game Lobby", titleStyle);
        titleLabel.setFontScale(1.3f);
        titleLabel.setAlignment(Align.center);
        card.add(titleLabel).padBottom(2).row();

        playerCountLabel = new Label("Connected Players: 1 / " + Constants.MAX_PLAYERS, skin);
        playerCountLabel.setColor(MenuUIHelper.TEXT_MUTED);
        playerCountLabel.setAlignment(Align.center);
        card.add(playerCountLabel).padBottom(14).row();

        // 3. 4 Player Slots Container (Direct table layout, no cramped scrollbars)
        playersTable = new Table(skin);
        playersTable.setBackground(skin.newDrawable("white", new Color(0.06f, 0.08f, 0.12f, 0.85f)));
        playersTable.pad(10, 14, 10, 14);
        playersTable.defaults().fillX().expandX().padBottom(6);

        card.add(playersTable).width(530).padBottom(14).row();

        // 4. Admin Controls
        Table adminControls = new Table();
        adminControls.defaults().pad(4);

        botPlayersCheckbox = new CheckBox(" Fill empty slots with Bots", skin);
        botPlayersCheckbox.setVisible(false);
        botPlayersCheckbox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateStartButtonState();
            }
        });

        loadGameButton = new TextButton("Load Saved Game", skin);
        loadGameButton.setVisible(false);
        loadGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.getGameStateManager().requestSaveFilesList();
            }
        });

        adminControls.add(botPlayersCheckbox).left().expandX();
        adminControls.add(loadGameButton).width(160).height(36).right();
        card.add(adminControls).width(530).padBottom(10).row();

        // 5. Status Label
        statusLabel = new Label("Waiting for players...", skin);
        statusLabel.setColor(MenuUIHelper.ACCENT_GOLD);
        statusLabel.setAlignment(Align.center);
        statusLabel.setWrap(true);
        card.add(statusLabel).width(530).padBottom(14).row();

        // 6. Start Button
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
        card.add(startButton).width(280).height(46).padBottom(8).row();

        rootTable.add(card).width(580);
        stage.addActor(rootTable);

        game.getGameStateManager().setLobbyScreen(this);
        checkAdminStatus();

        disconnectButton.setText("Leave Lobby");
        disconnectButton.setSize(140, 38);
        disconnectButton.setPosition(Gdx.graphics.getWidth() - 160, Gdx.graphics.getHeight() - 50);

        // Initial render of empty slots
        updatePlayersList(new ArrayList<>());
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        disconnectButton.setPosition(width - 160, height - 50);
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
                        game.getGameStateManager().startGame(botPlayersCheckbox.isChecked(), true, selectedFile);
                    }
                }
            }
        };

        dialog.getContentTable().pad(20);
        if (fileNames.isEmpty()) {
            dialog.text("No save files found on the server.");
        } else {
            dialog.getContentTable().add(new Label("Select a save file to load:", skin)).padBottom(10).row();
            ScrollPane scroll = new ScrollPane(saveList, skin);
            dialog.getContentTable().add(scroll).width(440).height(180).pad(10).row();

            TextButton loadButton = new TextButton("Load Selected", skin);
            dialog.button(loadButton, true);
        }

        TextButton cancelButton = new TextButton("Cancel", skin);
        dialog.button(cancelButton, false);

        dialog.getButtonTable().pad(10);
        dialog.getButtonTable().getCells().get(0).width(140).height(38).pad(8);
        if (!fileNames.isEmpty()) {
            dialog.getButtonTable().getCells().get(1).width(140).height(38).pad(8);
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

        // Render each of the 4 slots directly with ample width and no clipping
        for (int i = 0; i < Constants.MAX_PLAYERS; i++) {
            Table slotRow = new Table();
            slotRow.setBackground(skin.newDrawable("white", new Color(0.12f, 0.15f, 0.20f, 0.9f)));
            slotRow.pad(8, 14, 8, 14);

            if (i < this.players.size()) {
                Player p = this.players.get(i);
                Color color = getColorForName(p.getColor());

                // Colored swatch indicator
                Image colorDot = new Image(skin.newDrawable("white", color));
                slotRow.add(colorDot).size(18, 18).padRight(12);

                Label nameLabel = new Label(p.getName(), skin);
                nameLabel.setColor(MenuUIHelper.TEXT_PRIMARY);
                slotRow.add(nameLabel).left().expandX();

                boolean isHost = (i == 0);
                String tagText = p.getColor().toUpperCase() + (p instanceof BotPlayer ? " (Bot)" : (isHost ? " [HOST]" : ""));
                Label tagLabel = new Label(tagText, skin);
                tagLabel.setColor(color);
                tagLabel.setAlignment(Align.right);
                slotRow.add(tagLabel).right().padLeft(10);
            } else {
                // Empty slot waiting for players
                Image emptyDot = new Image(skin.newDrawable("white", new Color(0.3f, 0.35f, 0.45f, 0.5f)));
                slotRow.add(emptyDot).size(18, 18).padRight(12);

                Label waitingLabel = new Label("Slot " + (i + 1) + ": Waiting for player...", skin);
                waitingLabel.setColor(new Color(0.45f, 0.50f, 0.60f, 0.8f));
                slotRow.add(waitingLabel).left().expandX();

                Label openTag = new Label("[OPEN]", skin);
                openTag.setColor(new Color(0.40f, 0.45f, 0.55f, 0.7f));
                slotRow.add(openTag).right();
            }

            playersTable.add(slotRow).fillX().expandX().row();
        }

        playerCountLabel.setText("Connected Players: " + this.players.size() + " / " + Constants.MAX_PLAYERS);
        updateStartButtonState();
    }

    private Color getColorForName(String colorName) {
        return MenuUIHelper.getColorForName(colorName);
    }

    public void updateStartButtonState() {
        int playerCount = players.size();
        boolean hasBots = botPlayersCheckbox.isChecked();
        boolean enoughPlayers = playerCount >= 2 || (playerCount >= 1 && hasBots);

        if (isAdmin) {
            if (enoughPlayers) {
                statusLabel.setText("Ready to start match! Click 'Start Game' below.");
                statusLabel.setColor(MenuUIHelper.SUCCESS_GREEN);
            } else {
                statusLabel.setText("Waiting for players (need 2+ players, or check 'Fill with Bots')");
                statusLabel.setColor(MenuUIHelper.ACCENT_GOLD);
            }

            startButton.setDisabled(!enoughPlayers);
            startButton.setTouchable(enoughPlayers ? Touchable.enabled : Touchable.disabled);
        } else {
            statusLabel.setText("Waiting for the host to start the game...");
            statusLabel.setColor(MenuUIHelper.TEXT_MUTED);
            startButton.setDisabled(true);
        }
    }

    public void setFirstPlayer() {
        isAdmin = true;
        checkAdminStatus();
    }

    public void showError(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setColor(MenuUIHelper.ERROR_RED);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        game.getGameStateManager().setLobbyScreen(null);
    }
}
