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
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Timer;
import ludo.client.LudoGame;
import ludo.client.render.GameRenderer;
import ludo.client.render.Skybox;
import ludo.client.ui.GameHUD;
import ludo.core.entities.Pawn;
import ludo.core.entities.Player;
import ludo.core.game.GameState;

import java.util.*;
import java.util.logging.Logger;

public class GameScreen extends BaseScreen {
    private static final Logger LOGGER = Logger.getLogger(GameScreen.class.getName());
    private static final String TAG = "GameScreen";
    private static final float AUTO_SAVE_INTERVAL = 60f; // Auto-save every 60 seconds

    private final GameHUD hud;
    private final GameRenderer renderer;
    private final Skybox skybox;
    private final ModelBatch modelBatch;
    private final Environment environment;
    final PerspectiveCamera camera;
    private final TextButton rollButton;
    private final TextButton saveButton;
    private final List<Player> players;
    private final Vector3 cameraTarget = new Vector3(0, 0, 0);
    final float zoomSpeed = 2f;
    final float minZoom = 2f;
    final float maxZoom = 20f;
    final float minHeight = 0.5f;
    final float maxHeight = 30f;

    /// Game state
    private Player currentPlayer;
    private boolean canMove;
    private int lastDiceRoll;
    private boolean isRolling = false;
    private boolean isTopDownView = false;

    private Map<Integer, PawnUIState> pawnUIStates = new HashMap<>();
    private Queue<PendingAnimation> animationQueue = new LinkedList<>();
    private boolean processingAnimations = false;
    private GameState currentGameState = GameState.WAITING_FOR_PLAYERS;

    // Camera control variables
    float cameraRotation = 0;
    float cameraDistance = 14f;
    float cameraHeight = 10f;
    private float timeSinceLastAutoSave = 0f;

    private static class PawnUIState {
        boolean highlighted = false;
        boolean moving = false;
        boolean selectable = false;

        @Override
        public String toString() {
            return "PawnUIState{highlighted=" + highlighted +
                ", moving=" + moving +
                ", selectable=" + selectable + "}";
        }
    }

    private static class PendingAnimation {
        String color;
        int pawnIndex;
        int fromPosition;
        int toPosition;

        PendingAnimation(String color, int pawnIndex, int fromPosition, int toPosition) {
            this.color = color;
            this.pawnIndex = pawnIndex;
            this.fromPosition = fromPosition;
            this.toPosition = toPosition;
        }
    }

    public GameScreen(final LudoGame game) {
        super(game);

        this.players = new ArrayList<>();

        modelBatch = new ModelBatch();
        environment = new Environment();
        setupLighting();

        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        hud = new GameHUD(this);
        hud.updateTopDownButtonText(isTopDownView);

        Table actionButtonTable = new Table();
        actionButtonTable.setFillParent(true);
        actionButtonTable.align(Align.bottomRight); // Align the whole table to the bottom right
        actionButtonTable.pad(20);

        rollButton = new TextButton("Roll Dice", skin);
        saveButton = new TextButton("Save Game", skin);

        actionButtonTable.add(rollButton).width(150).height(40).padBottom(10).row();
        actionButtonTable.add(saveButton).width(150).height(40).padBottom(10).row();
        actionButtonTable.add(disconnectButton).width(150).height(40).row();

        rollButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!isRolling) {
                    Gdx.app.log(TAG, "Roll button clicked");
                    requestDiceRoll();
                }
            }
        });
        saveButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                performManualSave();
            }
        });

        this.renderer = new GameRenderer();

        this.skybox = new Skybox();

        stage.addActor(hud);
        stage.addActor(hud.getGameOverWindow()); // Get the window from the HUD and add it here.
        stage.addActor(actionButtonTable);

        setupCamera();

        setupInputHandling();
        this.currentGameState = GameState.IN_PROGRESS;
    }

    private void setupLighting() {
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));

        DirectionalLight mainLight = new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f);
        environment.add(mainLight);

        DirectionalLight fillLight = new DirectionalLight().set(0.3f, 0.3f, 0.3f, 1f, -0.4f, -0.2f);
        environment.add(fillLight);
    }

    private void setupCamera() {
        String playerColor = game.getGameStateManager().getCurrentColor();

        switch (playerColor.toUpperCase()) {
            case "YELLOW":
                cameraRotation = 0;
                break;
            case "BLUE":
                cameraRotation = 90;
                break;
            case "RED":
                cameraRotation = 180;
                break;
            case "GREEN":
                cameraRotation = 270;
                break;
            default:
                cameraRotation = 0; // Default fallback
                break;
        }

        cameraDistance = 14f;
        cameraHeight = 10f;

        updateCameraPosition();
        camera.near = 1f;
        camera.far = 300f;
        camera.update();
    }

    private void setupInputHandling() {
        inputMultiplexer.addProcessor(new GameInputProcessor(this));
    }

    public void toggleTopDownView() {
        isTopDownView = !isTopDownView;
        hud.updateTopDownButtonText(isTopDownView);
        updateCameraPosition();
    }

    private void handleContinuousInput(float delta) {
        if (isTopDownView) {
            return;
        }

        float rotationAmount = 100f * delta;
        float heightAmount = 5f * delta;

        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            cameraRotation += rotationAmount;
            updateCameraPosition();
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            cameraRotation -= rotationAmount;
            updateCameraPosition();
        }
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            cameraHeight = Math.min(maxHeight, cameraHeight + heightAmount);
            updateCameraPosition();
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            cameraHeight = Math.max(minHeight, cameraHeight - heightAmount);
            updateCameraPosition();
        }
    }


    void updateCameraPosition() {
        if (isTopDownView) {
            camera.position.set(0, 14, 0.01f); // Position high above
            camera.lookAt(0, 0, 0); // Look at the center of the board
        } else {
            double angleInRadians = Math.toRadians(cameraRotation + 225);

            float x = (float)(cameraDistance * Math.cos(angleInRadians));
            float z = (float)(cameraDistance * Math.sin(angleInRadians));

            camera.position.set(x, cameraHeight, z);
            camera.lookAt(cameraTarget);
        }
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

        processPendingAnimations();

//        Gdx.gl.glClearColor(0.2f, 0.2f, 0.3f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        skybox.render(camera);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

        camera.update();

        if (renderer != null && modelBatch != null && environment != null) {
            modelBatch.begin(camera);
            renderer.render(modelBatch, environment, delta);
            modelBatch.end();
        }

        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);

        if (stage != null) {
            stage.act(delta);
            stage.draw();
        }
    }

    private void processPendingAnimations() {
        if (processingAnimations || renderer.isAnimating() || animationQueue.isEmpty()) {
            return;
        }

        processingAnimations = true;
        PendingAnimation anim = animationQueue.poll();
        renderer.updatePawnPosition(anim.pawnIndex, anim.toPosition, anim.color);

        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                processingAnimations = false;
            }
        }, 0.2f);
    }

    private void performAutoSave() {
        game.getGameStateManager().requestSaveGame(true);
    }

    private void performManualSave() {
        game.getGameStateManager().requestSaveGame(false);
    }

    public void addPlayers(List<Player> newPlayers) {
        this.players.clear();
        this.players.addAll(newPlayers);
        renderer.createPawns(this.players);
        hud.updatePlayers(this.players);
    }


    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();

        rollButton.setPosition(width - 120, 20);
        saveButton.setPosition(width - 120, 80);
        disconnectButton.setPosition(width - 120, 140);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (modelBatch != null) modelBatch.dispose();
        if (renderer != null) renderer.dispose();
        if (skybox != null) skybox.dispose();

    }

    public void updatePlayerPawns(String color, List<Integer> positions) {
        for (Player player : players) {
            if (player.getColor().equalsIgnoreCase(color)) {
                for (int i = 0; i < positions.size(); i++) {
                    int newPosition = positions.get(i);
                    Pawn currentPawn = player.getPawns().get(i);
                    int oldPosition = currentPawn.getPosition();

                    if (newPosition != oldPosition) {
                        currentPawn.setPosition(newPosition);
//                        renderer.updatePawnPosition(i, newPosition, color);
                        animationQueue.add(new PendingAnimation(color, i, oldPosition, newPosition));
                    }
                }
                break;
            }
        }
    }

    public void setCurrentPlayer(String identifier) {
        for (Player player : players) {
            String playerColor = player.getColor().toUpperCase();
            String playerIdentifier = player.getName();

            if (playerColor.equals(identifier.toUpperCase()) || playerIdentifier.equals(identifier)) {
                currentPlayer = player;
                hud.updateCurrentPlayer(player.getColor());
                return;
            }
        }
        LOGGER.severe("Player not found for identifier: " + identifier);
    }

    public void updateDiceDisplay(int value) {
        lastDiceRoll = value;
        hud.updateDiceValue(value);
        renderer.updateDiceValue(value);
        isRolling = false; // Reset rolling state after dice animation is set
    }

    private boolean isMyTurn() {
        return currentPlayer != null && currentPlayer.getName().equals(game.getGameStateManager().getCurrentUsername());
    }

    public void showMessage(String message) {
        hud.showMessage(message);
    }

    public void enablePawnSelection() {
        canMove = true;
        rollButton.setDisabled(true);
    }

    public void enableRollButton() {
        rollButton.setDisabled(false);
        canMove = false;
    }

    public void enableControls() {
        if (lastDiceRoll == 0) {
            enableRollButton();
        } else {
            enablePawnSelection();
        }
    }

    public void disableControls() {
        rollButton.setDisabled(true);
        canMove = false;
    }

    public void showWinnerScreen(String winner) {
        hud.showWinnerScreen(winner);
    }

    public void playMoveAnimation(int pawnIndex, int newPosition) {
        if (currentPlayer != null) {
            String color = currentPlayer.getColor();
            renderer.updatePawnPosition(pawnIndex, newPosition, color);
        }
    }

    void handlePawnSelection(int screenX, int screenY) {
        if (!canMove || !isMyTurn()) {
            return;
        }

        int selectedPawnIndex = renderer.getPawnAtScreenCoords(screenX, screenY, camera, currentPlayer.getColor());

        if (selectedPawnIndex != -1) {
            game.getGameStateManager().requestMove(selectedPawnIndex);
        } else {
            LOGGER.info("No valid pawn selected at coordinates");
        }
    }

    private void requestDiceRoll() {
        if (!isMyTurn()) {
            LOGGER.warning("Attempted to roll dice when it's not player's turn");
            return;
        }
        isRolling = true;
        rollButton.setDisabled(true);
        renderer.startDiceRollAnimation();
        game.getGameStateManager().requestDiceRoll();
    }
}
