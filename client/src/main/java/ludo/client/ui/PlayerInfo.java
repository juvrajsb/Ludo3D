package ludo.client.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.Color;

/**
 * UI component that displays information about a player including their name and status
 */
public class PlayerInfo extends Table {
    private Label nameLabel;
    private Label statusLabel;
    private LabelStyle labelStyle;

    public PlayerInfo(String playerName) {
        // Initialize label style
        labelStyle = new LabelStyle(new BitmapFont(), Color.WHITE);

        // Create labels
        nameLabel = new Label(playerName, labelStyle);
        statusLabel = new Label("Waiting...", labelStyle);

        // Add to table
        add(nameLabel).padBottom(5);
        row();
        add(statusLabel);

        pad(10);
    }

    /**
     * Updates the player's status message
     * @param status new status message to display
     */
    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    /**
     * Updates the player's name
     * @param name new name to display
     */
    public void setName(String name) {
        nameLabel.setText(name);
    }
}

