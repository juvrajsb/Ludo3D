package ludo.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import ludo.core.entities.Player;

public class GameHUD extends Table {
    private final Label currentPlayerLabel;
    private final Label diceValueLabel;
    private final Label messageLabel;
    private final Table playerInfoTable;
    private final Table topPanel;
    private final Table bottomPanel;
    private final Window gameOverWindow;
    private final Skin skin;

    public GameHUD() {
        super();
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        // Make the HUD fill the screen
        setFillParent(true);
        setTouchable(Touchable.childrenOnly);

        // Create top panel for player info and game status
        topPanel = new Table(skin);
        topPanel.setBackground(skin.newDrawable("white"));
        topPanel.getBackground().setMinHeight(60);
        topPanel.setColor(0, 0, 0, 0.7f);

        // Create player info panel
        playerInfoTable = new Table(skin);
        playerInfoTable.pad(5);

        // Create labels with larger font size
        Label.LabelStyle labelStyle = new Label.LabelStyle(skin.get("default", Label.LabelStyle.class));
        labelStyle.font.getData().setScale(1.5f);

        currentPlayerLabel = new Label("Current Player: ", labelStyle);
        currentPlayerLabel.setColor(Color.WHITE);

        diceValueLabel = new Label("Dice: ", labelStyle);
        diceValueLabel.setColor(Color.WHITE);

        messageLabel = new Label("", labelStyle);
        messageLabel.setColor(Color.WHITE);
        messageLabel.setAlignment(Align.center);

        // Add elements to top panel
        topPanel.add(playerInfoTable).expandX().left().pad(10);
        topPanel.add(currentPlayerLabel).pad(10);
        topPanel.add(diceValueLabel).pad(10);

        // Create bottom panel
        bottomPanel = new Table(skin);
        bottomPanel.setBackground(skin.newDrawable("white"));
        bottomPanel.getBackground().setMinHeight(50);
        bottomPanel.setColor(0, 0, 0, 0.7f);

        // Add message label to bottom panel
        bottomPanel.add(messageLabel).expand().fill().pad(10);

        // Create game over window (initially hidden)
        gameOverWindow = new Window("Game Over", skin);
        gameOverWindow.setVisible(false);
        gameOverWindow.setModal(true);
        gameOverWindow.setMovable(false);

        // Add panels to main table
        add(topPanel).expandX().fillX().height(60).top().row();
        add().expand().row(); // This pushes the bottom panel to the bottom
        add(bottomPanel).expandX().fillX().height(50).bottom();

        // Add game over window
        addActor(gameOverWindow);
    }

    public void updateCurrentPlayer(String playerName) {
        if (playerName == null) return;
        currentPlayerLabel.setText("Current Player: " + playerName);

        // Update color based on player
        switch(playerName.toUpperCase()) {
            case "RED": currentPlayerLabel.setColor(Color.RED); break;
            case "BLUE": currentPlayerLabel.setColor(Color.BLUE); break;
            case "GREEN": currentPlayerLabel.setColor(Color.GREEN); break;
            case "YELLOW": currentPlayerLabel.setColor(Color.YELLOW); break;
            default: currentPlayerLabel.setColor(Color.WHITE);
        }
    }

    public void updateDiceValue(int value) {
        diceValueLabel.setText("Dice: " + value);
    }

    public void showMessage(String message) {
        messageLabel.setText(message);
    }

    public void updatePlayers(java.util.List<Player> players) {
        playerInfoTable.clear();
        for (Player player : players) {
            Label playerLabel = new Label(player.getName(), skin);
            switch(player.getColor().toUpperCase()) {
                case "RED": playerLabel.setColor(Color.RED); break;
                case "BLUE": playerLabel.setColor(Color.BLUE); break;
                case "GREEN": playerLabel.setColor(Color.GREEN); break;
                case "YELLOW": playerLabel.setColor(Color.YELLOW); break;
            }
            playerInfoTable.add(playerLabel).pad(5);
        }
    }

    public void showWinnerScreen(String winner) {
        gameOverWindow.clear();

        Table content = new Table(skin);
        Label winnerLabel = new Label(winner + " Wins!", skin);
        winnerLabel.setFontScale(2.0f);

        TextButton okButton = new TextButton("OK", skin);
        okButton.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                gameOverWindow.setVisible(false);
            }
        });

        content.add(winnerLabel).pad(20).row();
        content.add(okButton).pad(10).width(100);

        gameOverWindow.add(content);
        gameOverWindow.pack();
        gameOverWindow.setPosition(
            (Gdx.graphics.getWidth() - gameOverWindow.getWidth()) / 2,
            (Gdx.graphics.getHeight() - gameOverWindow.getHeight()) / 2
        );
        gameOverWindow.setVisible(true);
    }
}
