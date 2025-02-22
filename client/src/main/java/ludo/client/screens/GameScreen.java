package ludo.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Timer;
import ludo.client.LudoGame;
import ludo.client.render.GameRenderer;
import ludo.client.ui.GameHUD;
import ludo.core.entities.Player;
import ludo.core.persistence.GamePersistence;
import ludo.core.game.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class GameScreen extends BaseScreen {
    private static final Logger LOGGER = Logger.getLogger(GameScreen.class.getName());
    private static final String TAG = "GameScreen";
    private static final float AUTO_SAVE_INTERVAL = 60f; // Auto-save every 60 seconds

    private final GameHUD hud;
    private final GameRenderer renderer;
    private final ModelBatch modelBatch;
    private final Environment environment;
    final PerspectiveCamera camera;
    private final TextButton rollButton;
    private final List<Player> players;
    private final Vector3 cameraTarget = new Vector3(0, 0, 0);
    final float zoomSpeed = 2f;
    final float minZoom = 2f;
    final float maxZoom = 20f;
    final float minHeight = -2f;
    final float maxHeight = 30f;

    // Game state
    private Player currentPlayer;
    private boolean canMove;
    private int lastDiceRoll;
    private boolean isRolling = false;

    // Camera control variables
    float cameraRotation = 0;
    float cameraDistance = 14f;
    float cameraHeight = 10f;
    private float timeSinceLastAutoSave = 0f;

    public GameScreen(final LudoGame game) {
        super(game);
        hud = new GameHUD();

        this.players = new ArrayList<>();

        modelBatch = new ModelBatch();
        environment = new Environment();
        setupLighting();

        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        setupCamera();

        rollButton = new TextButton("Roll Dice", skin);
        rollButton.setSize(100, 50);
        rollButton.setPosition(Gdx.graphics.getWidth() - 120, 20);
        rollButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!isRolling) {
                    requestDiceRoll();
                }
            }
        });

        this.renderer = new GameRenderer(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        stage.addActor(hud);
        stage.addActor(rollButton);

        // createTestPanel();
        setupInputHandling();
        game.getGameStateManager().initialize(this);
    }

    private void setupLighting() {
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));

        DirectionalLight mainLight = new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f);
        environment.add(mainLight);

        DirectionalLight fillLight = new DirectionalLight().set(0.3f, 0.3f, 0.3f, 1f, -0.4f, -0.2f);
        environment.add(fillLight);
    }

    private void setupCamera() {
        updateCameraPosition();
        camera.near = 1f;
        camera.far = 300f;
        camera.update();
    }

    private void setupInputHandling() {
        inputMultiplexer.addProcessor(new GameInputProcessor(this));
    }

    private void handleContinuousInput(float delta) {
        float rotationAmount = 100f * delta;
        float heightAmount = 5f * delta;

        // Check for continuous keyboard input
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            cameraRotation += rotationAmount;
            updateCameraPosition();
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            cameraRotation -= rotationAmount;
            updateCameraPosition();
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            cameraHeight = Math.min(maxHeight, cameraHeight + heightAmount);
            updateCameraPosition();
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            cameraHeight = Math.max(minHeight, cameraHeight - heightAmount);
            updateCameraPosition();
        }

        // Alternative controls using WASD
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            cameraRotation += rotationAmount;
            updateCameraPosition();
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            cameraRotation -= rotationAmount;
            updateCameraPosition();
        }
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            cameraHeight = Math.min(maxHeight, cameraHeight + heightAmount);
            updateCameraPosition();
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            cameraHeight = Math.max(minHeight, cameraHeight - heightAmount);
            updateCameraPosition();
        }
    }

    void updateCameraPosition() {
        float x = - (float) (cameraDistance * Math.cos(Math.toRadians(cameraRotation))); //todo to fix
        float z = (float) (cameraDistance * Math.sin(Math.toRadians(cameraRotation)));
        camera.position.set(x, cameraHeight, z);
        camera.lookAt(cameraTarget);
        camera.up.set(Vector3.Y);
        camera.update();
    }

    @Override
    public void render(float delta) {
        handleContinuousInput(delta);

        timeSinceLastAutoSave += delta;
        if (timeSinceLastAutoSave >= AUTO_SAVE_INTERVAL && game.getGameStateManager().isGameStarted()) {
            performAutoSave();
            timeSinceLastAutoSave = 0f;
        }

        Gdx.gl.glClearColor(0.2f, 0.2f, 0.3f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

        camera.update();

        // Render 3D scene
        if (renderer != null && modelBatch != null && environment != null) {
            modelBatch.begin(camera);
            renderer.render(modelBatch, environment, delta);
            modelBatch.end();
        }

        // Disable depth testing for UI
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);

        // Render UI
        if (stage != null) {
            stage.act(delta);
            stage.draw();
        }
    }

    private void performAutoSave() {
        try {
            GamePersistence.autoSave(
                game.getPlayers(),
                game.getGameStateManager().getCurrentColor(),
                game.getGameStateManager().isGameStarted() ? GameState.IN_PROGRESS : GameState.WAITING_FOR_PLAYERS
            );
            LOGGER.fine("Auto-save completed successfully");
        } catch (Exception e) {
            LOGGER.warning("Failed to auto-save game: " + e.getMessage());
        }
    }

    public void addPlayer(Player player) { // TODO is this correct are players supposed to enter the game like this?
        if (!players.contains(player)) {
            players.add(player);
            renderer.createPawns(players);
            hud.updatePlayers(players);
        }
    }

    public int getPlayerCount() {//TODO check usage not used currently
        return players.size();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }

    @Override
    public void dispose() {
        super.dispose();
        modelBatch.dispose();
        renderer.dispose();
    }

//    public void updateGameState(String state) {
//        if (currentPlayer != null) {
//            renderer.updateAllPawnPositions();
//            hud.updateCurrentPlayer(currentPlayer.getColor());  // Use color instead of name
//        } else {
//            LOGGER.warning("Current player is null in updateGameState");
//        }
//        hud.showMessage(state);
//    }

    public void updatePlayerPawns(String color, List<Integer> positions) {
        // Only update pawns that have actually moved
        Gdx.app.log(TAG, "Inside updatePlayerPawns");
        for (Player player : players) {
            if (player.getColor().toUpperCase().equals(color)) {
                for (int i = 0; i < positions.size(); i++) {
                    renderer.updatePawnPosition(i, positions.get(i), color);
                }
                break;
            }
        }
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(String identifier) {
        LOGGER.info("Setting current player to: " + identifier);
        // Try finding player by color first
        for (Player player : players) {
            if (player.getColor().toUpperCase().equals(identifier.toUpperCase()) ||
                player.getName().equals(identifier)) {
                currentPlayer = player;
                hud.updateCurrentPlayer(player.getColor());
                boolean isMyTurn = player.getName().equals(game.getGameStateManager().getCurrentUsername());
                rollButton.setDisabled(!isMyTurn);
                // createTestPanel();

                LOGGER.info("Current player set - Name: " + player.getName() +
                    ", Color: " + player.getColor() +
                    ", isMyTurn: " + isMyTurn);
                return;
            }
        }
        LOGGER.severe("Player not found for identifier: " + identifier);
    }

    private void requestDiceRoll() {
        isRolling = true;
        rollButton.setDisabled(true);
        game.getGameStateManager().requestDiceRoll();
    }

    public void updateDiceDisplay(int value) {
        lastDiceRoll = value;

        hud.updateDiceValue(value);

        renderer.updateDiceValue(value);

        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                isRolling = false;
                if (currentPlayer != null &&
                    currentPlayer.getName().equals(game.getGameStateManager().getCurrentUsername())) {
                    rollButton.setDisabled(false);
                    canMove = true;
                }
            }
        }, 0.5f);
    }

    // private void updateGameState() { //TODO check usage not used currently
    //     if (currentPlayer != null) {
    //         renderer.updateAllPawnPositions();
    //         hud.updateCurrentPlayer(currentPlayer.getName());
    //     }
    // }

    public void showMessage(String message) {
        hud.showMessage(message);
    }

    public void enablePawnSelection() {
        LOGGER.info("=== Enable Pawn Selection ===");
        LOGGER.info("Before enable - canMove: " + canMove);
        canMove = true;
        disableControls();
        showMessage("Select a pawn to move");
        LOGGER.info("After enable - canMove: " + canMove);
    }

    public void removePlayer(String playerName) {
        for (Player player : game.getPlayers()) {
            if (player.getName().equals(playerName)) {
                game.getPlayers().remove(player);
                break;
            }
        }
        renderer.createPawns(game.getPlayers());
    }

    public void enableControls() {
        canMove = false;
        isRolling = false;
        rollButton.setDisabled(false);
        showMessage("Your turn! Roll the dice");
    }

    public void disableControls() {
        canMove = false;
        rollButton.setDisabled(true);
    }

    public void showWinnerScreen(String winner) {
        hud.showWinnerScreen(winner);
    }

    public void playMoveAnimation(int pawnIndex, int newPosition) {
        Gdx.app.log(TAG, String.format("Inside playMoveAnimation"));
        String color = null;
        int playerIndex = pawnIndex / 4;
        if (playerIndex < players.size()) {
            color = players.get(playerIndex).getColor();
        }

        if (color != null) {
            renderer.updatePawnPosition(pawnIndex, newPosition, color);
        }
    }

    // private void createTestPanel() {
    //     Table testPanel = new Table(skin);
    //     testPanel.setPosition(30, 10);  // Bottom left corner

    //     Label title = new Label("Debug Controls", skin);
    //     testPanel.add(title).row();

    //     // Get current color from GameState
    //     String currentColor = game.getGameStateManager().getCurrentColor();

    //     // Create 4 buttons for the pawns
    //     for (int i = 0; i < 4; i++) {
    //         final int pawnIndex = i;
    //         TextButton pawnButton = new TextButton("Select " + currentColor + " Pawn " + i, skin);
    //         pawnButton.addListener(new ChangeListener() {
    //             @Override
    //             public void changed(ChangeEvent event, Actor actor) {
    //                 Gdx.app.log("GameScreen", "Test button selecting pawn " + pawnIndex);
    //                 // This bypasses ray casting and directly sends the move request
    //                 if (canMove) {
    //                     game.getGameStateManager().requestMove(pawnIndex);
    //                     canMove = false;
    //                 }
    //             }
    //         });
    //         testPanel.add(pawnButton).pad(5).row();
    //     }

    //     stage.addActor(testPanel);
    // }

    void handlePawnSelection(int screenX, int screenY) {
        LOGGER.info("=== Pawn Selection Handler Start ===");
        LOGGER.info("Selection state - canMove: " + canMove +
            ", currentPlayer: " + (currentPlayer != null ? currentPlayer.getColor() : "null"));

        if (!canMove || currentPlayer == null ||
            !currentPlayer.getName().equals(game.getGameStateManager().getCurrentUsername())) {
            LOGGER.info("Selection blocked - canMove: " + canMove +
                ", currentPlayer: " + (currentPlayer != null ? currentPlayer.getName() : "null") +
                ", currentUsername: " + game.getGameStateManager().getCurrentUsername());
            return;
        }

        int selectedPawn = renderer.getPawnAtScreenCoords(screenX, screenY, camera, currentPlayer.getColor());
        if (selectedPawn != -1) {
            LOGGER.info("Valid pawn selected: " + selectedPawn + " for player " + currentPlayer.getColor());
            game.getGameStateManager().requestMove(selectedPawn);
            canMove = false;
//        } else {
//            Vector3 worldPos = new Vector3(screenX, screenY, 0);
//            camera.unproject(worldPos);
//            Gdx.app.log(TAG, "No valid pawn selected at world position: " + worldPos);
//        }
        } else {
            LOGGER.info("No valid pawn selected at coordinates: " + screenX + "," + screenY);
        }
        LOGGER.info("=== Pawn Selection Handler End ===");
    }
}
