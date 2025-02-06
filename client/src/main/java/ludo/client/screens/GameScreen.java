package ludo.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import ludo.client.LudoGame;
import ludo.client.render.GameRenderer;
import ludo.client.ui.GameHUD;
import ludo.core.entities.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class GameScreen extends BaseScreen {
    private static final Logger LOGGER = Logger.getLogger(GameScreen.class.getName());
    private static final String TAG = "GameScreen";

    private final GameHUD hud;
    private final GameRenderer renderer;
    private final ModelBatch modelBatch;
    private final Environment environment;
    private final PerspectiveCamera camera;
    private final TextButton rollButton;
    private final List<Player> players;
    private final Vector3 cameraTarget = new Vector3(0, 0, 0);
    private final float zoomSpeed = 2f;
    private final float minZoom = 5f;
    private final float maxZoom = 20f;
    private final float minHeight = 2f;
    private final float maxHeight = 20f;
    // Game state
    private Player currentPlayer;
    private boolean canMove;
    private int lastDiceRoll;
    private boolean isRolling = false;
    // Camera control variables
    private float cameraRotation = 0;
    private float cameraDistance = 12f;
    private float cameraHeight = 8f;

    public GameScreen(final LudoGame game) {
        super(game);
        hud = new GameHUD();

        this.players = new ArrayList<>();

        // Initialize 3D rendering components
        modelBatch = new ModelBatch();
        environment = new Environment();
        setupLighting();

        // Set up camera
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        setupCamera();

        // Create roll button
        rollButton = new TextButton("Roll Dice", skin);
        rollButton.setSize(100, 50);
        rollButton.setPosition(Gdx.graphics.getWidth() - 120, 20);
        rollButton.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.log(TAG, "Roll button clicked");
                if (!isRolling) {
                    Gdx.app.log(TAG, "Requesting dice roll");
                    requestDiceRoll();
                }
            }
        });

        this.renderer = new GameRenderer(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        stage.addActor(hud);
        stage.addActor(rollButton);

        setupInputHandling();
        game.getGameStateManager().initialize(this);

        Gdx.app.log(TAG, "GameScreen initialized");
    }

    private void setupLighting() {
        // Ambient light
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));

        // Main directional light
        DirectionalLight mainLight = new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f);
        environment.add(mainLight);

        // Added a secondary light for better illumination
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
        inputMultiplexer.addProcessor(new GameInputProcessor());

        Gdx.app.log(TAG, "Input processors initialized. Count: " + inputMultiplexer.getProcessors().size);
    }

//    public void updatePawnHighlights() { //TODO check usage
//        if (canMove && currentPlayer != null &&
//            currentPlayer.getName().equals(game.getGameStateManager().getCurrentUsername())) {
//            renderer.highlightSelectablePawns(currentPlayer, lastDiceRoll);
//        } else {
//            renderer.clearHighlights();
//        }
//    }

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

    private void updateCameraPosition() {
        float x = (float) (cameraDistance * Math.cos(Math.toRadians(cameraRotation)));
        float z = (float) (cameraDistance * Math.sin(Math.toRadians(cameraRotation)));
        camera.position.set(x, cameraHeight, z);
        camera.lookAt(cameraTarget);
        camera.up.set(Vector3.Y);
        camera.update();
    }

    @Override
    public void render(float delta) {
        handleContinuousInput(delta);

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

    public void addPlayer(Player player) { // TODO is this correct are players supposed to enter the game like this?
        Gdx.app.log(TAG, "Adding player to GameScreen: " + player.getName());
        if (!players.contains(player)) {
            players.add(player);
            renderer.createPawns(players);
            hud.updatePlayers(players);
            Gdx.app.log(TAG, "Players after add: " + players.size());
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

    public void updateGameState(String state) {
        LOGGER.info("Updating game state: " + state);
        if (currentPlayer != null) {
            LOGGER.info("Current player in update: " + currentPlayer.getName() +
                " (" + currentPlayer.getColor() + ")");
            renderer.updateAllPawnPositions();
            hud.updateCurrentPlayer(currentPlayer.getColor());  // Use color instead of name
        } else {
            LOGGER.warning("Current player is null in updateGameState");
        }
        hud.showMessage(state);
    }

    public void updatePlayerPawns(String color, List<Integer> positions) {
        for (Player player : players) {
            if (player.getColor().equals(color)) {
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

    public void setCurrentPlayer(String playerName) {
        LOGGER.info("Setting current player to: " + playerName);
        for (Player player : players) {
            if (player.getColor().equals(playerName)) { //changed to color
                currentPlayer = player;
                hud.updateCurrentPlayer(player.getColor());
                boolean isMyTurn = player.getName().equals(game.getGameStateManager().getCurrentUsername());
                rollButton.setDisabled(!isMyTurn);

                LOGGER.info("Current player set - Name: " + player.getName() +
                    ", Color: " + player.getColor() +
                    ", isMyTurn: " + isMyTurn);
                break;
            }
        }
    }

    private void requestDiceRoll() {
        isRolling = true;
        rollButton.setDisabled(true);
        game.getGameStateManager().requestDiceRoll();
    }

    public void updateDiceDisplay(int value) {
        lastDiceRoll = value;

        // Update the HUD display
        hud.updateDiceValue(value);

        // Trigger the 3D dice roll animation
        renderer.updateDiceValue(value);

        if (currentPlayer != null && currentPlayer.getName().equals(game.getGameStateManager().getCurrentUsername())) {
            rollButton.setDisabled(false);
            canMove = true;
        }
    }

    private void updateGameState() { //TODO check usage not used currently
        if (currentPlayer != null) {
            renderer.updateAllPawnPositions();
            hud.updateCurrentPlayer(currentPlayer.getName());
        }
    }

    public void showMessage(String message) {
        hud.showMessage(message);
    }

    public void enablePawnSelection() {
        canMove = true;
        showMessage("Select a pawn to move");
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
        String color = null;
        int playerIndex = pawnIndex / 4;
        if (playerIndex < players.size()) {
            color = players.get(playerIndex).getColor();
        }

        if (color != null) {
            renderer.updatePawnPosition(pawnIndex, newPosition, color);
        }
    }

    private void handlePawnSelection(int screenX, int screenY) {
        if (!canMove || currentPlayer == null || !currentPlayer.getName().equals(game.getGameStateManager().getCurrentUsername())) {
            Gdx.app.log(TAG, "Pawn selection disabled - " +
                (!canMove ? "canMove is false" :
                    currentPlayer == null ? "currentPlayer is null" :
                        "not current player's turn"));
            return;
        }

        // Convert screen coordinates to ray
        Ray pickRay = camera.getPickRay(screenX, screenY);
        Gdx.app.log(TAG, String.format("Pick ray - Origin: %s, Direction: %s",
            pickRay.origin, pickRay.direction));

        if (currentPlayer == null || !currentPlayer.getName().equals(game.getGameStateManager().getCurrentUsername())) {
            Gdx.app.log(TAG, "Not current player's turn");
            return;
        }

//        renderer.debugRayTest(screenX, screenY, camera);

        int selectedPawn = renderer.getPawnAtScreenCoords(screenX, screenY, camera);
        if (selectedPawn != -1) {
            game.getGameStateManager().requestMove(selectedPawn);
            canMove = false;
        } else {
            Gdx.app.log(TAG, "No pawn selected");
        }
    }


    public void updatePawnPosition(int pawnIndex, int newPosition) { //TODO check usage not used currently
        String color = null;
        int playerIndex = pawnIndex / 4;
        if (playerIndex < players.size()) {
            Player player = players.get(playerIndex);
            color = player.getColor();
        }

        if (color != null) {
            renderer.updatePawnPosition(pawnIndex, newPosition, color);
        }
    }

    private class GameInputProcessor extends InputAdapter {
        private static final float DRAG_THRESHOLD = 5f;
        private final Vector3 touchPoint = new Vector3();
        private final boolean isMacOS = System.getProperty("os.name").toLowerCase().contains("mac");
        private boolean isDragging = false;
        private float startX, startY;

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            // Log raw input for debugging
            Gdx.app.log(TAG, String.format("Raw input - touchDown: x=%d, y=%d, button=%d, isMac=%b",
                screenX, screenY, button, isMacOS));

            // Map button codes for macOS
            int mappedButton = button;
            if (isMacOS) {
                switch (button) {
                    case 0:
                        mappedButton = Input.Buttons.LEFT;
                        break;
                    case 1:
                        mappedButton = Input.Buttons.RIGHT;
                        break;
                }
            }

            // Only process left mouse button
            if (mappedButton != Input.Buttons.LEFT) {
                return false;
            }

            startX = screenX;
            startY = screenY;
            isDragging = false;

            // Convert screen coordinates to world coordinates
            touchPoint.set(screenX, screenY, 0);
            camera.unproject(touchPoint);

            Gdx.app.log(TAG, String.format("Processed click - World: (%f, %f, %f)",
                touchPoint.x, touchPoint.y, touchPoint.z));

            return true;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            // Map button codes for macOS
            int mappedButton = isMacOS ? (button == 0 ? Input.Buttons.LEFT : button) : button;

            if (mappedButton != Input.Buttons.LEFT) {
                return false;
            }

            float dx = Math.abs(screenX - startX);
            float dy = Math.abs(screenY - startY);
            boolean wasDrag = (dx * dx + dy * dy) > DRAG_THRESHOLD * DRAG_THRESHOLD;

            if (!wasDrag && !isDragging) {
                touchPoint.set(screenX, screenY, 0);
                camera.unproject(touchPoint);

                Gdx.app.log(TAG, "Processing click for pawn selection");
                handlePawnSelection(screenX, screenY);
            }

            isDragging = false;
            return true;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            if (!isDragging) {
                float dx = Math.abs(screenX - startX);
                float dy = Math.abs(screenY - startY);
                isDragging = (dx * dx + dy * dy) > DRAG_THRESHOLD * DRAG_THRESHOLD;
            }

            if (isDragging) {
                float deltaX = (screenX - startX) * 0.5f;
                float deltaY = (screenY - startY) * 0.5f;

                cameraRotation += deltaX * 0.2f;
                cameraHeight = Math.max(minHeight, Math.min(maxHeight, cameraHeight + deltaY * 0.1f));

                updateCameraPosition();
                startX = screenX;
                startY = screenY;
            }

            return true;
        }

        @Override
        public boolean scrolled(float amountX, float amountY) {
            float newDistance = cameraDistance + amountY * zoomSpeed;
            newDistance = Math.max(minZoom, Math.min(maxZoom, newDistance));

            if (newDistance != cameraDistance) {
                cameraDistance = newDistance;
                updateCameraPosition();
            }

            return true;
        }
    }
}
