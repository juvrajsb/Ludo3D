package ludo.client.ui;

import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import ludo.core.entities.Player;
import java.util.List;

/**
 * This class is responsible for the game HUD.
 * It extends the Table class.
 */
public class GameHUD extends Table {
    private final Label currentPlayerLabel;
    private final Label messageLabel;
    private final Label diceValueLabel;
    private final Table playerInfoTable;
    private final Skin skin;
    private final Window winnerWindow;
    private final Table controlsTable;

    public GameHUD() {
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        // Setup main layout
        setFillParent(true);
        pad(20);

        // Create UI components
        currentPlayerLabel = new Label("Current Player: ", skin);
        messageLabel = new Label("", skin);
        diceValueLabel = new Label("ClientDice: ", skin);
        playerInfoTable = new Table(skin);
        controlsTable = new Table(skin);

        // Setup winner window (initially hidden)
        winnerWindow = new Window("Game Over", skin);
        winnerWindow.setVisible(false);
        winnerWindow.setModal(true);
        winnerWindow.setMovable(false);

        setupLayout();
    }

    private void setupLayout() {
        // Add player info section at top
        add(playerInfoTable).expandX().fillX().pad(10).row();

        // Add current player and dice info
        Table infoTable = new Table();
        infoTable.add(currentPlayerLabel).pad(5);
        infoTable.add(diceValueLabel).pad(5);
        add(infoTable).expandX().fillX().pad(10).row();

        // Add message display in middle
        add(messageLabel).expandX().fillX().pad(10).row();

        // Add controls at bottom
        add(controlsTable).expandX().fillX().pad(10);
    }

    public void updateDiceValue(int value) {
        diceValueLabel.setText("Dice: " + value);
    }

    public void updateCurrentPlayer(String playerName) {
        currentPlayerLabel.setText("Current Player: " + playerName);
    }

    public void updateGameState(String state) {
        messageLabel.setText(state);
    }

    public void showMessage(String message) {
        messageLabel.setText(message);
    }

    public void updatePlayers(List<Player> players) {
        playerInfoTable.clear();
        for (Player player : players) {
            Label playerLabel = new Label(player.getName() + " (" + player.getColor() + ")", skin);
            playerLabel.setColor(getColorForPlayer(player.getColor()));
            playerInfoTable.add(playerLabel).pad(5);
        }
    }

    public void enableControls() {
        controlsTable.setVisible(true);
    }

    public void disableControls() {
        controlsTable.setVisible(false);
    }

    public void showWinnerScreen(String winner) {
        winnerWindow.clear();

        Label winnerLabel = new Label(winner + " wins!", skin);
        TextButton okButton = new TextButton("OK", skin);

        winnerWindow.add(winnerLabel).pad(20).row();
        winnerWindow.add(okButton).pad(10);

        okButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                winnerWindow.setVisible(false);
            }
        });

        winnerWindow.setVisible(true);
        winnerWindow.setPosition(
            (Gdx.graphics.getWidth() - winnerWindow.getWidth()) / 2,
            (Gdx.graphics.getHeight() - winnerWindow.getHeight()) / 2
        );
    }

    private Color getColorForPlayer(String colorName) {
        switch (colorName.toLowerCase()) {
            case "red": return Color.RED;
            case "blue": return Color.BLUE;
            case "green": return Color.GREEN;
            case "yellow": return Color.YELLOW;
            default: return Color.WHITE;
        }
    }
}
