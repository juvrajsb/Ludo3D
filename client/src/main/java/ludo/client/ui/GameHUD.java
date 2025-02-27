package ludo.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Timer;
import ludo.core.entities.Player;
import java.util.List;

public class GameHUD extends Table {
    private final Label currentPlayerLabel;
    private final Label diceValueLabel;
    private final Label messageLabel;
    private final Table playerInfoTable;
    private final Table topPanel;
    private final Table bottomPanel;

//    private final Label networkStatusLabel;
//    private final ProgressBar networkStatusBar;
//    private final Table statusPanel;
    private final TextTooltip messageTooltip;
//    private final Dialog confirmationDialog;

    private final Window gameOverWindow;
    private final Skin skin;

    public GameHUD() {
        super();
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        setFillParent(true);
        setTouchable(Touchable.childrenOnly);

        topPanel = new Table(skin);
        topPanel.setBackground(skin.newDrawable("white"));
        topPanel.getBackground().setMinHeight(60);
        topPanel.setColor(0, 0, 0, 0.7f);

        playerInfoTable = new Table(skin);
        playerInfoTable.pad(5);

        Label.LabelStyle labelStyle = new Label.LabelStyle(skin.get("default", Label.LabelStyle.class));
        labelStyle.font.getData().setScale(1.5f);

        currentPlayerLabel = new Label("Current Player: ", labelStyle);
        currentPlayerLabel.setColor(Color.WHITE);

        diceValueLabel = new Label("Dice: ", labelStyle);
        diceValueLabel.setColor(Color.WHITE);

        messageLabel = new Label("", labelStyle);
        messageLabel.setColor(Color.WHITE);
        messageLabel.setAlignment(Align.center);

        topPanel.add(playerInfoTable).expandX().left().pad(10);
        topPanel.add(currentPlayerLabel).pad(10);
        topPanel.add(diceValueLabel).pad(10);

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

        add(topPanel).expandX().fillX().height(60).top().row();
        add().expand().row();
        add(bottomPanel).expandX().fillX().height(50).bottom();

        addActor(gameOverWindow);

//        networkStatusLabel = new Label("Connected", skin);
//        networkStatusLabel.setColor(Color.GREEN);
//
//        networkStatusBar = new ProgressBar(0, 100, 1, false, skin);
//        networkStatusBar.setValue(100);
//        networkStatusBar.setColor(Color.GREEN);

//        statusPanel = new Table(skin);
//        statusPanel.setBackground(skin.newDrawable("white"));
//        statusPanel.setColor(0, 0, 0, 0.5f);

//        statusPanel.add(new Label("Network:", skin)).padRight(5);
//        statusPanel.add(networkStatusLabel).padRight(10);
//        statusPanel.add(networkStatusBar).width(50);

//        add(statusPanel).expand().right().top().pad(10);
//        row();

        messageTooltip = new TextTooltip("", skin);
//        messageLabel.addListener(messageTooltip);

//        confirmationDialog = new Dialog("Confirm", skin);
//        confirmationDialog.button("Yes", true);
//        confirmationDialog.button("No", false);
    }

    public void updateCurrentPlayer(String playerColor) {
        if (playerColor == null) return;
        currentPlayerLabel.setText("Current Player: " + playerColor);

        switch(playerColor.toUpperCase()) {
            case "RED": currentPlayerLabel.setColor(Color.RED); break;
            case "BLUE": currentPlayerLabel.setColor(Color.BLUE); break;
            case "GREEN": currentPlayerLabel.setColor(Color.GREEN); break;
            case "YELLOW": currentPlayerLabel.setColor(Color.YELLOW); break;
            default: currentPlayerLabel.setColor(Color.WHITE);
        }
    }

//    public void updateNetworkStatus(boolean connected, int latency) {
//        if (connected) {
//            // Update status based on latency
//            if (latency < 50) {
//                networkStatusLabel.setText("Excellent");
//                networkStatusLabel.setColor(Color.GREEN);
//                networkStatusBar.setValue(100);
//                networkStatusBar.setColor(Color.GREEN);
//            } else if (latency < 100) {
//                networkStatusLabel.setText("Good");
//                networkStatusLabel.setColor(Color.LIME);
//                networkStatusBar.setValue(75);
//                networkStatusBar.setColor(Color.LIME);
//            } else if (latency < 200) {
//                networkStatusLabel.setText("Fair");
//                networkStatusLabel.setColor(Color.YELLOW);
//                networkStatusBar.setValue(50);
//                networkStatusBar.setColor(Color.YELLOW);
//            } else {
//                networkStatusLabel.setText("Poor");
//                networkStatusLabel.setColor(Color.ORANGE);
//                networkStatusBar.setValue(25);
//                networkStatusBar.setColor(Color.ORANGE);
//            }
//        } else {
//            networkStatusLabel.setText("Disconnected");
//            networkStatusLabel.setColor(Color.RED);
//            networkStatusBar.setValue(0);
//            networkStatusBar.setColor(Color.RED);
//        }
//    }

    public void showMessage(String message) {
        messageLabel.setText(message);

        if (message.contains("Error") || message.contains("Invalid") ||
            message.contains("Cannot") || message.contains("Disconnected")) {
            messageLabel.setColor(Color.RED);
            messageTooltip.getActor().setText(message + "\nSee console for details");
            flashMessage();
        } else if (message.contains("Your turn") || message.contains("Roll again")) {
            messageLabel.setColor(Color.GREEN);
        } else {
            messageLabel.setColor(Color.WHITE);
        }
    }

    private void flashMessage() {
        final Color originalColor = messageLabel.getColor().cpy();

        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                messageLabel.setColor(Color.RED);
            }
        }, 0);

        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                messageLabel.setColor(Color.WHITE);
            }
        }, 0.3f);

        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                messageLabel.setColor(Color.RED);
            }
        }, 0.6f);

        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                messageLabel.setColor(originalColor);
            }
        }, 0.9f);
    }

//    public void showConfirmation(String message, Runnable onConfirm) {
//        confirmationDialog.getContentTable().clear();
//        confirmationDialog.text(message);
//        confirmationDialog.getButtonTable().clear();
//
//        TextButton yesButton = new TextButton("Yes", skin);
//        TextButton noButton = new TextButton("No", skin);
//
//        confirmationDialog.button(yesButton, true);
//        confirmationDialog.button(noButton, false);
//
//        confirmationDialog.show(getStage());
//
//        // Set up listeners
//        yesButton.addListener(new ChangeListener() {
//            @Override
//            public void changed(ChangeEvent event, Actor actor) {
//                confirmationDialog.hide();
//                if (onConfirm != null) {
//                    onConfirm.run();
//                }
//            }
//        });
//
//        noButton.addListener(new ChangeListener() {
//            @Override
//            public void changed(ChangeEvent event, Actor actor) {
//                confirmationDialog.hide();
//            }
//        });
//    }

    public void updateDiceValue(int value) {
        diceValueLabel.setText("Dice: " + value);
    }

    public void updatePlayers(List<Player> players) {
        playerInfoTable.clear();

        Table headerRow = new Table(skin);
        headerRow.add(new Label("Player", skin)).width(100);
        headerRow.add(new Label("Color", skin)).width(80);
        headerRow.add(new Label("Status", skin)).width(80);
        playerInfoTable.add(headerRow).pad(5).row();

        for (Player player : players) {
            Table playerRow = new Table(skin);

            Label playerLabel = new Label(player.getName(), skin);
            Label colorLabel = new Label(player.getColor(), skin);
            Label statusLabel = new Label("Active", skin);

            // Set colors
            Color playerColor = getColorForName(player.getColor());
            colorLabel.setColor(playerColor);

            playerRow.add(playerLabel).width(100);
            playerRow.add(colorLabel).width(80);
            playerRow.add(statusLabel).width(80);

            playerInfoTable.add(playerRow).pad(5).row();
        }
    }

    private Color getColorForName(String colorName) {
        switch(colorName.toUpperCase()) {
            case "RED": return Color.RED;
            case "BLUE": return Color.BLUE;
            case "GREEN": return Color.GREEN;
            case "YELLOW": return Color.YELLOW;
            default: return Color.WHITE;
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
