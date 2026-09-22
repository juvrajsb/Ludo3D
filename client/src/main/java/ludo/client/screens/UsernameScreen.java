package ludo.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import ludo.client.LudoGame;
import ludo.client.ui.MenuUIHelper;
import ludo.core.events.serverToClient.Response;

import java.util.HashMap;
import java.util.Map;

public class UsernameScreen extends BaseScreen {
    private TextField usernameField;
    private Label errorLabel;
    private String selectedColor = "Red";
    private final Map<String, Table> colorCards = new HashMap<>();
    private final Map<String, Label> selectBadges = new HashMap<>();
    private Image activeColorPreviewDot;
    private Label activeColorPreviewLabel;
    private boolean joinInProgress = false;

    public UsernameScreen(final LudoGame game) {
        super(game);

        if (!game.getGameStateManager().isConnected()) {
            game.setScreen(new ConnectionScreen(game));
            return;
        }

        createUI();
        disconnectButton.setText("Disconnect");
        disconnectButton.setPosition(Gdx.graphics.getWidth() - 140, Gdx.graphics.getHeight() - 50);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        disconnectButton.setPosition(width - 140, height - 50);
    }

    private void createUI() {
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

        Label titleLabel = new Label("Player Setup", titleStyle);
        titleLabel.setFontScale(1.3f);
        titleLabel.setAlignment(Align.center);
        card.add(titleLabel).padBottom(4).row();

        Label subtitleLabel = new Label("Enter your display name and select your pawn color", skin);
        subtitleLabel.setColor(MenuUIHelper.TEXT_MUTED);
        subtitleLabel.setAlignment(Align.center);
        card.add(subtitleLabel).padBottom(18).row();

        // 3. Username Input
        Table userRow = new Table();
        Label nameLabel = new Label("Player Name:", skin);
        nameLabel.setColor(MenuUIHelper.TEXT_MUTED);
        userRow.add(nameLabel).right().padRight(12);

        usernameField = new TextField("", skin);
        usernameField.setMessageText("Enter your name...");
        userRow.add(usernameField).width(240).height(38).left();
        card.add(userRow).padBottom(18).row();

        // 4. Color Selection Label
        Label colorPrompt = new Label("Select Pawn Color:", skin);
        colorPrompt.setColor(MenuUIHelper.TEXT_MUTED);
        card.add(colorPrompt).padBottom(8).row();

        // 4 Color Cards (2x2 grid for generous clickable size)
        Table colorsGrid = new Table();
        colorsGrid.defaults().pad(6);

        String[] colors = new String[]{"Red", "Blue", "Green", "Yellow"};
        int col = 0;
        for (String c : colors) {
            final String colorName = c;
            Table colorCard = createColorCard(colorName);
            colorCards.put(colorName, colorCard);
            colorsGrid.add(colorCard).width(190).height(46);
            col++;
            if (col % 2 == 0) {
                colorsGrid.row();
            }
        }
        card.add(colorsGrid).padBottom(12).row();

        // Active Color Banner Preview
        Table previewBanner = new Table();
        previewBanner.setBackground(skin.newDrawable("white", new Color(0.06f, 0.08f, 0.12f, 0.85f)));
        previewBanner.pad(6, 16, 6, 16);

        activeColorPreviewDot = new Image(skin.newDrawable("white", MenuUIHelper.getColorForName(selectedColor)));
        previewBanner.add(activeColorPreviewDot).size(16, 16).padRight(10);

        activeColorPreviewLabel = new Label("Selected Color: " + selectedColor.toUpperCase(), skin);
        activeColorPreviewLabel.setColor(MenuUIHelper.getColorForName(selectedColor));
        previewBanner.add(activeColorPreviewLabel);

        card.add(previewBanner).width(390).padBottom(14).row();
        updateColorCards();

        // 5. Error Label
        errorLabel = new Label("", skin);
        errorLabel.setColor(MenuUIHelper.ERROR_RED);
        errorLabel.setAlignment(Align.center);
        errorLabel.setWrap(true);
        card.add(errorLabel).width(390).padBottom(12).row();

        // 6. Action Button
        TextButton joinButton = new TextButton("Enter Game Lobby", skin);
        joinButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                attemptJoin();
            }
        });
        card.add(joinButton).width(280).height(46).padBottom(8).row();

        rootTable.add(card).width(480);
        stage.addActor(rootTable);
    }

    private Table createColorCard(final String colorName) {
        final Table card = new Table();
        card.setTouchable(Touchable.enabled);
        card.pad(8, 12, 8, 12);

        Color color = MenuUIHelper.getColorForName(colorName);

        // Bright color swatch box
        Image swatch = new Image(skin.newDrawable("white", color));
        card.add(swatch).size(22, 22).padRight(10);

        // Name
        Label nameLbl = new Label(colorName, skin);
        nameLbl.setColor(Color.WHITE);
        card.add(nameLbl).left().expandX();

        // Badge indicator
        Label badge = new Label("", skin);
        badge.setColor(MenuUIHelper.ACCENT_GOLD);
        badge.setFontScale(0.85f);
        card.add(badge).right();
        selectBadges.put(colorName, badge);

        card.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectColor(colorName);
            }
        });

        return card;
    }

    private void selectColor(String colorName) {
        selectedColor = colorName;
        updateColorCards();
    }

    private void updateColorCards() {
        for (Map.Entry<String, Table> entry : colorCards.entrySet()) {
            String cName = entry.getKey();
            Table card = entry.getValue();
            boolean isSelected = cName.equalsIgnoreCase(selectedColor);
            Color baseColor = MenuUIHelper.getColorForName(cName);

            if (isSelected) {
                // Highlighted active card with color tint
                card.setBackground(skin.newDrawable("white", new Color(baseColor.r * 0.7f, baseColor.g * 0.7f, baseColor.b * 0.7f, 0.55f)));
                selectBadges.get(cName).setText("[ACTIVE]");
            } else {
                card.setBackground(skin.newDrawable("white", new Color(0.12f, 0.15f, 0.20f, 0.9f)));
                selectBadges.get(cName).setText("");
            }
        }

        Color activeColor = MenuUIHelper.getColorForName(selectedColor);
        if (activeColorPreviewDot != null) {
            activeColorPreviewDot.setColor(activeColor);
        }
        if (activeColorPreviewLabel != null) {
            activeColorPreviewLabel.setText("Selected Color: " + selectedColor.toUpperCase());
            activeColorPreviewLabel.setColor(activeColor);
        }
    }

    private void attemptJoin() {
        if (joinInProgress) {
            return;
        }

        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            errorLabel.setText("Please enter a username to proceed.");
            return;
        }

        errorLabel.setText("Joining lobby...");
        errorLabel.setColor(MenuUIHelper.ACCENT_GOLD);
        joinInProgress = true;

        game.getGameStateManager().joinGame(username, selectedColor);
    }

    public void showReconnectDialog(final String playerName) {
        joinInProgress = false;

        Dialog dialog = new Dialog("Reconnect to Existing Match?", skin) {
            @Override
            protected void result(Object object) {
                if (Boolean.TRUE.equals(object)) {
                    game.getGameStateManager().sendReconnectRequest(playerName);
                } else {
                    errorLabel.setText("Please choose a different username.");
                    errorLabel.setColor(MenuUIHelper.ERROR_RED);
                    joinInProgress = false;
                }
            }
        };

        dialog.getContentTable().pad(20);
        Label textLabel = new Label("An active disconnected game for player '" + playerName + "' was found.\nWould you like to reconnect?", skin);
        textLabel.setWrap(true);
        textLabel.setAlignment(Align.center);
        dialog.getContentTable().add(textLabel).width(420).padBottom(15);

        TextButton yesButton = new TextButton("Yes, Reconnect", skin);
        TextButton noButton = new TextButton("No, Choose Another Name", skin);
        dialog.button(yesButton, true);
        dialog.button(noButton, false);

        dialog.getButtonTable().pad(10);
        dialog.getButtonTable().getCells().get(0).width(160).height(40).padRight(10);
        dialog.getButtonTable().getCells().get(1).width(200).height(40);

        dialog.show(stage);
    }

    public void onJoinResponse(Response response) {
        joinInProgress = false;
        errorLabel.setColor(MenuUIHelper.ERROR_RED);

        switch (response) {
            case OK:
            case FIRST_PLAYER:
                break;
            case COLOR_TAKEN:
                errorLabel.setText("The color '" + selectedColor + "' is already taken. Please choose another.");
                break;
            case USERNAME_TAKEN:
                errorLabel.setText("The username '" + usernameField.getText().trim() + "' is already taken.");
                break;
            case GAME_FULL:
                errorLabel.setText("This game is already full (maximum 4 players).");
                break;
            default:
                errorLabel.setText("Failed to join game: " + response);
                break;
        }
    }
}
