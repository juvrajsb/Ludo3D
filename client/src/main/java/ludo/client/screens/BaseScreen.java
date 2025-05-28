package ludo.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import ludo.client.LudoGame;
import java.util.logging.Logger;

/**
 * This class is responsible for the base screen.
 * It is an abstract class that implements the Screen interface.
 */
public abstract class BaseScreen implements Screen {
    protected final LudoGame game;
    protected Stage stage;
    protected InputMultiplexer inputMultiplexer;
    protected Viewport viewport;
    protected Skin skin;
    protected TextButton disconnectButton;
    private static final Logger LOGGER = Logger.getLogger(BaseScreen.class.getName());

    public BaseScreen(LudoGame game) {
        this.game = game;
        this.viewport = new ScreenViewport();
        this.stage = new Stage(viewport);
        this.inputMultiplexer = new InputMultiplexer(stage);
        this.skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        // Create disconnect button
        disconnectButton = new TextButton("Disconnect", skin);
        disconnectButton.setSize(120, 40);
        disconnectButton.setPosition(Gdx.graphics.getWidth() - 130, 10); // Position in top-right corner
        disconnectButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                handleDisconnect();
            }
        });
        stage.addActor(disconnectButton);
    }

    protected void handleDisconnect() {
        if (game.getGameStateManager() != null) {
            LOGGER.info("Starting disconnection process...");

            // Send disconnection event to server
            try {
//                LOGGER.info("Preparing disconnection event...");
                ludo.core.events.clientToServer.ClientDisconnectedEvent disconnectEvent =
                    new ludo.core.events.clientToServer.ClientDisconnectedEvent(
                        game.getGameStateManager().getCurrentUsername()
                    );

//                LOGGER.info("Sending disconnection event to server...");
                game.getGameStateManager().getNetworkHandler().sendMessage(disconnectEvent);
                LOGGER.info("Disconnection event sent successfully");

                // Wait for acknowledgment with timeout
                long startTime = System.currentTimeMillis();
                long timeout = 2000; // 2 seconds timeout
                boolean acknowledged = false;

//                LOGGER.info("Waiting for disconnection acknowledgment...");
                while (System.currentTimeMillis() - startTime < timeout) {
                    if (game.getGameStateManager().isDisconnectionAcknowledged()) {
                        acknowledged = true;
                        LOGGER.info("Received disconnection acknowledgment from server");
                        break;
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        LOGGER.warning("Disconnection wait interrupted: " + e.getMessage());
                        break;
                    }
                }

                if (!acknowledged) {
                    LOGGER.warning("Disconnection acknowledgment timeout after " +
                        (System.currentTimeMillis() - startTime) + "ms - proceeding with disconnect");
                }
            } catch (Exception e) {
                LOGGER.severe("Error during disconnection process: " + e.getMessage());
            } finally {
//                LOGGER.info("Stopping outgoing queue processor...");
                game.getGameStateManager().getNetworkHandler().stopOutgoingQueueProcessor();

//                LOGGER.info("Performing final disconnect cleanup...");
                try {
                    // Ensure we're not in the middle of sending/receiving messages
                    Thread.sleep(100);

                    game.getGameStateManager().getNetworkHandler().disconnect();
                    LOGGER.info("Disconnected from server, returning to connection screen");
                } catch (Exception e) {
                    LOGGER.severe("Error during final disconnect: " + e.getMessage());
                }
                game.setScreen(new ConnectionScreen(game));
            }
        } else {
            LOGGER.warning("Cannot disconnect - GameStateManager is null");
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        stage.getViewport().update(width, height, true);
        // Update disconnect button position
        disconnectButton.setPosition(width - 130, 10);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void render(float delta) {
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
