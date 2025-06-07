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
import ludo.core.entities.Pawn;
import ludo.core.entities.Player;
import ludo.core.persistence.GamePersistence;
import ludo.core.game.GameState;

import java.util.*;
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
    private final TextButton saveButton;
    private final List<Player> players;
    private final Vector3 cameraTarget = new Vector3(0, 0, 0);
    final float zoomSpeed = 2f;
    final float minZoom = 2f;
    final float maxZoom = 20f;
    final float minHeight = -2f;
    final float maxHeight = 30f;

    /// Game state
    private Player currentPlayer;
    private boolean canMove;
    private int lastDiceRoll;
    private boolean isRolling = false;

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
//                LOGGER.info("Roll button clicked - isRolling: " + isRolling +
//                    ", canMove: " + canMove +
//                    ", isMyTurn: " + (currentPlayer != null &&
//                    currentPlayer.getName().equals(game.getGameStateManager().getCurrentUsername())));

                if (!isRolling) {
                    Gdx.app.log(TAG, "Roll button clicked");
                    requestDiceRoll();
                }
            }
        });

        saveButton = new TextButton("Save Game", skin);
        saveButton.setSize(100, 50);
        saveButton.setPosition(Gdx.graphics.getWidth() - 120, 80);
        saveButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                performManualSave();
            }
        });

        this.renderer = new GameRenderer(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        disconnectButton.setPosition(Gdx.graphics.getWidth() - 120, 200);

        stage.addActor(hud);
        stage.addActor(rollButton);
        stage.addActor(saveButton);

        setupInputHandling();
        game.getGameStateManager().initialize(this);
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
        cameraRotation = 0;
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

    private void handleContinuousInput(float delta) {
        float rotationAmount = 100f * delta;
        float heightAmount = 5f * delta;

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
        double angleInRadians = Math.toRadians(cameraRotation + 225);

        float x = (float)(cameraDistance * Math.cos(angleInRadians));
        float z = (float)(cameraDistance * Math.sin(angleInRadians));

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

        processPendingAnimations();

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

    private void processPendingAnimations() {
        if (processingAnimations) {
            return;
        }

        if (!animationQueue.isEmpty() && !renderer.isAnimating()) {
            processingAnimations = true;
            PendingAnimation anim = animationQueue.poll();

            renderer.updatePawnPosition(anim.pawnIndex, anim.toPosition, anim.color);

            Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    processingAnimations = false;
                }
            }, 0.2f); // Adjust timing based on your animation duration
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

    private void performManualSave() {
        try {
            // Get the current game state
            GameState state = game.getGameStateManager().isGameStarted() ?
                GameState.IN_PROGRESS : GameState.WAITING_FOR_PLAYERS;

            // Get the current player color
            String currentColor = currentPlayer != null ? currentPlayer.getColor() : null;

            // Get the list of players from GameStateManager instead of LudoGame
            List<Player> gamePlayers = game.getGameStateManager().getCurrentPlayers();

//            LOGGER.info("=== Starting Manual Save ===");
//            LOGGER.info("Current game state: " + state);
//            LOGGER.info("Current player: " + (currentPlayer != null ?
//                currentPlayer.getName() + " (" + currentPlayer.getColor() + ")" : "null"));
//            LOGGER.info("Number of players: " + (gamePlayers != null ? gamePlayers.size() : "null"));

//            if (gamePlayers != null) {
//                for (Player player : gamePlayers) {
////                    LOGGER.info("Player: " + player.getName() +
////                        ", Color: " + player.getColor() +
////                        ", Pawns: " + (player.getPawns() != null ? player.getPawns().size() : "null"));
//                }
//            }

            GamePersistence.saveGame(gamePlayers, currentColor, state);
            showMessage("Game saved successfully");
            LOGGER.info("Manual save completed successfully");
        } catch (Exception e) {
            showMessage("Failed to save game");
            LOGGER.warning("Failed to save game: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void addPlayer(Player player) { // TODO is this correct are players supposed to enter the game like this?
        if (!players.contains(player)) {
            players.add(player);
            renderer.createPawns(players);
            hud.updatePlayers(players);
        }
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
        modelBatch.dispose();
        renderer.dispose();
    }

    public void updatePlayerPawns(String color, List<Integer> positions) {
        for (Player player : players) {
            if (player.getColor().equalsIgnoreCase(color)) {
                for (int i = 0; i < positions.size(); i++) {
                    int newPosition = positions.get(i);
                    int oldPosition = player.getPawns().get(i).getPosition();

                    if (newPosition != oldPosition) {
                        player.getPawns().get(i).setPosition(newPosition);
                        animationQueue.add(new PendingAnimation(
                            color, i, oldPosition, newPosition
                        ));

//                        LOGGER.info(String.format("Queued animation for %s pawn %d: %d -> %d",
//                            color, i, oldPosition, newPosition));
                    }
                }
                break;
            }
        }
    }

    public void highlightPawnAsMoving(int pawnIndex) {
        PawnUIState state = pawnUIStates.computeIfAbsent(pawnIndex, k -> new PawnUIState());
        state.moving = true;

        renderer.setPawnMovingState(pawnIndex, true);
    }

//    public void clearPawnMovingState(int pawnIndex) {
//        PawnUIState state = pawnUIStates.get(pawnIndex);
//        if (state != null) {
//            state.moving = false;
//
//            renderer.setPawnMovingState(pawnIndex, false);
//        }
//    }
//
//    public void setSelectablePawns(List<Integer> selectablePawnIndices) {
//        for (PawnUIState state : pawnUIStates.values()) {
//            state.selectable = false;
//        }
//
//        for (Integer index : selectablePawnIndices) {
//            PawnUIState state = pawnUIStates.computeIfAbsent(index, k -> new PawnUIState());
//            state.selectable = true;
//        }
//
//        renderer.setSelectablePawns(selectablePawnIndices);
//    }


    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(String identifier) {
//        LOGGER.info("Setting current player to: " + identifier);

        for (Player player : players) {
            if (player.getColor().toUpperCase().equals(identifier.toUpperCase()) ||
                player.getName().equals(identifier)) {
                currentPlayer = player;
                hud.updateCurrentPlayer(player.getColor());
                boolean isMyTurn = player.getName().equals(game.getGameStateManager().getCurrentUsername());

                rollButton.setDisabled(!isMyTurn);
//                LOGGER.info("Roll button disabled state set to: " + rollButton.isDisabled());

                if (isMyTurn) {
                    enableControls();
                } else {
                    disableControls();
                }

//                LOGGER.info("Current player set - Name: " + player.getName() +
//                    ", Color: " + player.getColor() +
//                    ", isMyTurn: " + isMyTurn);
                return;
            }
        }
        LOGGER.severe("Player not found for identifier: " + identifier);
    }

    public void updateDiceDisplay(int value) {
        lastDiceRoll = value;
//        LOGGER.info("Updating dice display - value: " + value + ", isMyTurn: " + isMyTurn());

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

    public void updateGameState(GameState state) {
        this.currentGameState = state;

        switch (state) {
            case WAITING_FOR_PLAYERS:
                showMessage("Waiting for players to join...");
                disableControls();
                break;

            case IN_PROGRESS:
                if (isMyTurn()) {
                    showMessage("Your turn! Roll the dice");
                    enableControls();
                } else {
                    showMessage("Waiting for " + (currentPlayer != null ? currentPlayer.getColor() : "other player"));
                    disableControls();
                }
                break;

            case DICE_ROLLED:
                showMessage("Dice rolled: " + lastDiceRoll);
                disableControls();
                break;

            case WAITING_FOR_MOVE:
                if (isMyTurn()) {
                    showMessage("Select a pawn to move");
                    enablePawnSelection();
                } else {
                    showMessage("Waiting for " + currentPlayer.getColor() + " to move");
                    disableControls();
                }
                break;

            case PLAYER_MOVED:
                disableControls();
                break;

            case GAME_OVER:
                showMessage("Game over!");
                disableControls();
                break;

            case DISCONNECTED:
                showMessage("Disconnected from server. Reconnecting...");
                disableControls();
                break;
        }
    }

    private boolean isMyTurn() {
        return currentPlayer != null &&
            currentPlayer.getName().equals(game.getGameStateManager().getCurrentUsername());
    }

    public void showMessage(String message) {
        hud.showMessage(message);
    }

    public void enablePawnSelection() {
//        LOGGER.info("=== Enable Pawn Selection ===");
//        LOGGER.info("Before enable - canMove: " + canMove);
        canMove = true;
        rollButton.setDisabled(true);
//        LOGGER.info("After enable - canMove: " + canMove);
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
//        LOGGER.info("Enabling controls - Current dice value: " + lastDiceRoll);
        if (lastDiceRoll == 0) {
            rollButton.setDisabled(false);
            canMove = false;
        } else {
            rollButton.setDisabled(true);
            canMove = true;
        }
    }

    public void enableRollButton() {
//        LOGGER.info("Enabling roll button");
        rollButton.setDisabled(false);
        canMove = false;
    }

    public void disableControls() {
//        LOGGER.info("Disabling all controls");
        rollButton.setDisabled(true);
        canMove = false;
    }

    public void showWinnerScreen(String winner) {
        hud.showWinnerScreen(winner);
    }

    public void playMoveAnimation(int pawnIndex, int newPosition) {
//        Gdx.app.log(TAG, String.format("Inside playMoveAnimation"));

        Player currentPlayer = getCurrentPlayer();
        String color = currentPlayer.getColor();

        Gdx.app.log(TAG, "Moving pawn for player: " + color + ", pawn index: " + pawnIndex);

        renderer.updatePawnPosition(pawnIndex, newPosition, color);
    }

//     private void createTestPanel() {
//         Table testPanel = new Table(skin);
//         testPanel.setPosition(30, 10);  // Bottom left corner
//
//         Label title = new Label("Debug Controls", skin);
//         testPanel.add(title).row();
//
//         String currentColor = game.getGameStateManager().getCurrentColor();
//
//         for (int i = 0; i < 4; i++) {
//             final int pawnIndex = i;
//             TextButton pawnButton = new TextButton("Select " + currentColor + " Pawn " + i, skin);
//             pawnButton.addListener(new ChangeListener() {
//                 @Override
//                 public void changed(ChangeEvent event, Actor actor) {
//                     Gdx.app.log("GameScreen", "Test button selecting pawn " + pawnIndex);
//                     // This bypasses ray casting and directly sends the move request
//                     if (canMove) {
//                         game.getGameStateManager().requestMove(pawnIndex);
//                         canMove = false;
//                     }
//                 }
//             });
//             testPanel.add(pawnButton).pad(5).row();
//         }
//
//         stage.addActor(testPanel);
//     }

    void handlePawnSelection(int screenX, int screenY) {
//        LOGGER.info("=== Pawn Selection Attempt ===");
//        LOGGER.info("Screen coordinates: " + screenX + ", " + screenY);
//        LOGGER.info("Current state - canMove: " + canMove +
//            ", currentPlayer: " + (currentPlayer != null ? currentPlayer.getName() : "null") +
//            ", isMyTurn: " + (currentPlayer != null && currentPlayer.getName().equals(game.getGameStateManager().getCurrentUsername())) +
//            ", lastDiceRoll: " + lastDiceRoll +
//            ", gameState: " + currentGameState);

        if (!canMove || currentPlayer == null ||
            !currentPlayer.getName().equals(game.getGameStateManager().getCurrentUsername())) {
//            LOGGER.info("Pawn selection disabled - canMove: " + canMove +
//                ", currentPlayer: " + (currentPlayer != null ? currentPlayer.getName() : "null"));
            return;
        }

        int selectedPawn = renderer.getPawnAtScreenCoords(screenX, screenY, camera, currentPlayer.getColor());
        LOGGER.info("Selected pawn index: " + selectedPawn);

        if (selectedPawn != -1) {
            Pawn pawn = currentPlayer.getPawns().get(selectedPawn);
            boolean isHome = pawn.isHome();
            boolean canLeaveHome = lastDiceRoll == 6 && isHome;

            LOGGER.info("Pawn validation - Index: " + selectedPawn +
                ", Position: " + pawn.getPosition() +
                ", isHome: " + isHome +
                ", canLeaveHome: " + canLeaveHome +
                ", lastDiceRoll: " + lastDiceRoll);

            if (canLeaveHome || !isHome) {
//                LOGGER.info("Valid pawn selection - requesting move for pawn " + selectedPawn);
                highlightPawnAsMoving(selectedPawn);
                game.getGameStateManager().requestMove(selectedPawn);
                canMove = false;
            } else {
                LOGGER.info("Invalid pawn selection - cannot move pawn " + selectedPawn);
                showMessage("Cannot move this pawn");
                renderer.flashInvalidSelection(selectedPawn);
            }
        } else {
            LOGGER.info("No valid pawn selected at coordinates");
        }
    }

    private void requestDiceRoll() {
        boolean isPlayersTurn = currentPlayer != null &&
            currentPlayer.getName().equals(game.getGameStateManager().getCurrentUsername());

//        LOGGER.info("requestDiceRoll called - isPlayersTurn: " + isPlayersTurn +
//            ", rollButton disabled: " + rollButton.isDisabled());

        if (!isPlayersTurn) {
            LOGGER.warning("Attempted to roll dice when it's not player's turn");
            return;
        }

        isRolling = true;
        rollButton.setDisabled(true);

        renderer.startDiceRollAnimation();

        game.getGameStateManager().requestDiceRoll();
    }

    public void reset() {
        LOGGER.info("Resetting game screen");
        players.clear();
        currentPlayer = null;
        canMove = false;
        lastDiceRoll = 0;
        isRolling = false;
        pawnUIStates.clear();
        animationQueue.clear();
        processingAnimations = false;
        currentGameState = GameState.WAITING_FOR_PLAYERS;
        hud.reset();
    }
}
