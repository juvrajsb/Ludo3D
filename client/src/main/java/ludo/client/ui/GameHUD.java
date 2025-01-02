package ludo.client.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.Color;

/**
 * Class that represents the Heads Up Display (HUD) for the game.
 * Contains game status information like current player, dice results, etc.
 */
public class GameHUD extends Table {
    private Label currentPlayerLabel;
    private DiceUI diceUI;
    private Label statusLabel;
    private LabelStyle labelStyle;

    public GameHUD() {
        // Initialize label style
        labelStyle = new LabelStyle(new BitmapFont(), Color.WHITE);

        // Initialize components
        currentPlayerLabel = new Label("Current Player: ", labelStyle);
        diceUI = new DiceUI();
        statusLabel = new Label("Waiting for game to start...", labelStyle);

        // Layout setup using Table (LibGDX's layout system)
        add(currentPlayerLabel).padRight(10);
        add(diceUI).size(50);
        row();
        add(statusLabel).colspan(2).padTop(10);

        // Table properties
        setFillParent(true);
        pad(10);
    }

    /**
     * Updates the current player display
     * @param playerName name of the current player
     */
    public void setCurrentPlayer(String playerName) {
        currentPlayerLabel.setText("Current Player: " + playerName);
    }

    /**
     * Updates the dice display
     * @param value dice value to display (1-6)
     */
    public void updateDice(int value) {
        diceUI.showFace(value);
    }

    /**
     * Updates the status message
     * @param message status message to display
     */
    public void setStatus(String message) {
        statusLabel.setText(message);
    }
}

