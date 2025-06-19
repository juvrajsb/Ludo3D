package ludo.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import ludo.client.screens.GameScreen;
import ludo.core.entities.Player;
import java.util.List;

public class GameHUD extends Table {
    private final Label currentPlayerLabel;
    private final Label diceValueLabel;
    private final HorizontalGroup playerInfoGroup;
    private final Window gameOverWindow;
    private final Skin skin;
    private final TextButton topDownViewButton;
    private Label messageLabel;

    public GameHUD(final GameScreen gameScreen) {
        super();
        this.skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        setFillParent(true);
        align(Align.top);

        // --- Top Panel ---
        Table topPanel = new Table();
        topPanel.setBackground(skin.newDrawable("white", 0, 0, 0, 0.6f));

        playerInfoGroup = new HorizontalGroup();
        playerInfoGroup.space(15);

        currentPlayerLabel = new Label("Current Player:", skin, "subtitle");
        diceValueLabel = new Label("Dice: -", skin, "default");

        topDownViewButton = new TextButton("2D/3D", skin);
        topDownViewButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                gameScreen.toggleTopDownView();
            }
        });


        topPanel.add(playerInfoGroup).expandX().left().padLeft(15);
        topPanel.add(currentPlayerLabel).pad(0, 50, 0, 10);
        topPanel.add(diceValueLabel).padRight(15);
        topPanel.add(topDownViewButton).padRight(15);

        add(topPanel).expandX().fillX().height(60).top().row();

        // --- Game Over Window (initially hidden) ---
        gameOverWindow = new Window("Game Over", skin);
        gameOverWindow.setVisible(false);
        gameOverWindow.setModal(true);
        gameOverWindow.setMovable(false);
    }

    public Window getGameOverWindow() {
        return gameOverWindow;
    }

    public void updateCurrentPlayer(String playerColor) {
        if (playerColor == null) return;
        currentPlayerLabel.setText(playerColor);
        currentPlayerLabel.setColor(getColorForName(playerColor));
    }

    public void updateDiceValue(int value) {
        diceValueLabel.setText("Dice: " + value);
    }

    public void updateTopDownButtonText(boolean isTopDown) {
        if (isTopDown) {
            topDownViewButton.setText("3D View");
        } else {
            topDownViewButton.setText("2D View");
        }
    }

    public void updatePlayers(List<Player> players) {
        playerInfoGroup.clear();
        for (Player player : players) {
            Table playerWidget = new Table();
            Image colorIndicator = new Image(skin.getDrawable("white"));
            colorIndicator.setColor(getColorForName(player.getColor()));

            Label nameLabel = new Label(player.getName(), skin);

            playerWidget.add(colorIndicator).size(20);
            playerWidget.add(nameLabel).padLeft(5);
            playerInfoGroup.addActor(playerWidget);
        }
    }

    public void showMessage(String text) {
        if (messageLabel != null) {
            messageLabel.remove();
        }

        messageLabel = new Label(text, skin, "default");
        messageLabel.setAlignment(Align.center);

        if (text.contains("Error") || text.contains("Invalid") || text.contains("Cannot")) {
            messageLabel.setColor(Color.RED);
        } else if (text.contains("Your turn") || text.contains("Wins!")) {
            messageLabel.setColor(Color.GREEN);
        } else {
            messageLabel.setColor(Color.WHITE);
        }

        messageLabel.setPosition(Gdx.graphics.getWidth() / 2, 80, Align.center);

        messageLabel.addAction(Actions.sequence(
            Actions.delay(3.0f),
            Actions.fadeOut(0.5f),
            Actions.removeActor()
        ));

        // Add the message label to this HUD table, not the stage
        this.addActor(messageLabel);
    }

    public void showWinnerScreen(String winner) {
        gameOverWindow.clear();
        Label winnerLabel = new Label(winner + " Wins!", skin, "window");
        winnerLabel.setFontScale(1.5f);
        winnerLabel.setColor(getColorForName(winner));
        winnerLabel.setAlignment(Align.center);

        TextButton okButton = new TextButton("OK", skin);
        okButton.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                gameOverWindow.setVisible(false);
            }
        });

        gameOverWindow.add(winnerLabel).pad(40).row();
        gameOverWindow.add(okButton).pad(20).width(100);
        gameOverWindow.pack();
        gameOverWindow.setPosition(
            (Gdx.graphics.getWidth() - gameOverWindow.getWidth()) / 2,
            (Gdx.graphics.getHeight() - gameOverWindow.getHeight()) / 2
        );
        gameOverWindow.setVisible(true);
    }

    private Color getColorForName(String colorName) {
        if (colorName == null) return Color.WHITE;
        switch(colorName.toUpperCase()) {
            case "RED": return Color.RED;
            case "BLUE": return Color.ROYAL;
            case "GREEN": return Color.LIME;
            case "YELLOW": return Color.YELLOW;
            default: return Color.WHITE;
        }
    }

    public void reset() {
        currentPlayerLabel.setText("Current Player:");
        diceValueLabel.setText("Dice: -");
        playerInfoGroup.clear();
        if (messageLabel != null) {
            messageLabel.remove();
        }
        gameOverWindow.setVisible(false);
    }
}
