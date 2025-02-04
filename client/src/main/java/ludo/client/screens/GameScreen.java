package ludo.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.math.Vector3;
import ludo.client.LudoGame;
import ludo.client.assets.GameAssets;
import ludo.client.render.GameRenderer;
import ludo.client.ui.GameHUD;
import ludo.core.entities.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameScreen extends BaseScreen {
    private static final String TAG = "GameScreen";
    private final GameHUD hud;
    private final GameRenderer renderer;
    private final ModelBatch modelBatch;
    private final Environment environment;
    private final PerspectiveCamera camera;
    private final TextButton rollButton;
    private List<Player> players = new CopyOnWriteArrayList<>();

    // Game state
    private Player currentPlayer;
    private int selectedPawnIndex = -1;
    private boolean canMove;
    private int lastDiceRoll;
    private boolean isRolling = false;

    // Camera control variables
    private float cameraRotation = 0;
    private float cameraDistance = 12f;
    private float cameraHeight = 8f;
    private Vector3 cameraTarget = new Vector3(0, 0, 0);
    private boolean isDragging = false;
    private float lastTouchX;
    private float lastTouchY;

    public GameScreen(final LudoGame game) {
        super(game);
        Gdx.app.log(TAG, "Initializing GameScreen");
        Gdx.app.log(TAG, "GameStateManager has " + game.getGameStateManager().getCurrentPlayers().size() + " players");
        this.players = new ArrayList<>();

        // Initialize 3D rendering components
        modelBatch = new ModelBatch();
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        // Set up camera
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        updateCameraPosition();
        camera.near = 1f;
        camera.far = 300f;
        camera.update();

        // Initialize UI components
        this.hud = new GameHUD();

        // Create roll button
        rollButton = new TextButton("Roll Dice", skin);
        rollButton.setSize(100, 50);
        rollButton.setPosition(Gdx.graphics.getWidth() - 120, 20);
        stage.addActor(rollButton);

        // Initialize renderer with correct dimensions
        this.renderer = new GameRenderer(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Set up input handling
        setupInputHandling();

        game.getGameStateManager().initialize(this);

        // Debug log for initialization
        Gdx.app.log(TAG, "GameScreen initialized");
    }

    private void setupInputHandling() {
        Gdx.input.setInputProcessor(new InputMultiplexer(stage, new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                lastTouchX = screenX;
                lastTouchY = screenY;
                isDragging = true;
                return true;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                isDragging = false;
                return true;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (isDragging) {
                    float deltaX = (screenX - lastTouchX) * 0.5f;
                    float deltaY = (screenY - lastTouchY) * 0.5f;

                    cameraRotation += deltaX;
                    cameraHeight = Math.max(2f, Math.min(20f, cameraHeight - deltaY * 0.1f));

                    updateCameraPosition();

                    lastTouchX = screenX;
                    lastTouchY = screenY;
                }
                return true;
            }

            @Override
            public boolean scrolled(float amountX, float amountY) {
                cameraDistance = Math.max(5f, Math.min(20f, cameraDistance + amountY));
                updateCameraPosition();
                return true;
            }
        }));
    }

    private void updateCameraPosition() {
        float x = (float)(cameraDistance * Math.cos(Math.toRadians(cameraRotation)));
        float z = (float)(cameraDistance * Math.sin(Math.toRadians(cameraRotation)));
        camera.position.set(x, cameraHeight, z);
        camera.lookAt(cameraTarget);
        camera.up.set(Vector3.Y);
        camera.update();
    }

    @Override
    public void render(float delta) {
        // Clear screen
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.3f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // Update camera
        camera.update();

        // Debug logging periodically
        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            Gdx.app.log(TAG, "Camera Position: " + camera.position);
            Gdx.app.log(TAG, "Number of players: " + players.size());
            Gdx.app.log(TAG, "Number of pawns: " + renderer.getNumberOfPawns());
        }

        // Render 3D scene
        if (renderer != null && modelBatch != null && environment != null) {
            modelBatch.begin(camera);
            renderer.render(modelBatch, environment);
            modelBatch.end();
        }

        // Render UI
        if (stage != null) {
            stage.act(delta);
            stage.draw();
        }
    }

//    @Override
    public void addPlayer(Player player) {
        Gdx.app.log(TAG, "Adding player to GameScreen: " + player.getName());
        if (!players.contains(player)) {
            players.add(player);
            renderer.createPawns(players);
            Gdx.app.log(TAG, "Players after add: " + players.size());
        }
    }

    public int getPlayerCount() {
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
        if (currentPlayer != null) {
            renderer.updatePawnPositions();
            hud.updateCurrentPlayer(currentPlayer.getName());
        }
        hud.showMessage(state);
    }

    public void updatePlayerPawns(String color, List<Integer> positions) {
        for (Player player : players) {
            if (player.getColor().equals(color)) {
                for (int i = 0; i < positions.size(); i++) {
                    renderer.updatePawnPosition(i, positions.get(i));
                }
                break;
            }
        }
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(String playerName) {
        for (Player player : players) {
            if (player.getName().equals(playerName)) {
                currentPlayer = player;
                hud.updateCurrentPlayer(player.getName());
                rollButton.setDisabled(!player.getName().equals(game.getGameStateManager().getCurrentUsername()));
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
        hud.updateDiceValue(value);
        isRolling = false;
        if (currentPlayer != null && currentPlayer.getName().equals(game.getGameStateManager().getCurrentUsername())) {
            rollButton.setDisabled(false);
            canMove = true;
        }
    }

    private void updateGameState() {
        if (currentPlayer != null) {
            renderer.updatePawnPositions();
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
        canMove = true;
        rollButton.setDisabled(false);
    }

    public void disableControls() {
        canMove = false;
        rollButton.setDisabled(true);
    }

    public void showWinnerScreen(String winner) {
        Dialog winnerDialog = new Dialog("Game Over", skin);
        winnerDialog.text(winner + " wins!");
        winnerDialog.button("OK");
        winnerDialog.show(stage);
    }

    public void playMoveAnimation(int pawnIndex, int newPosition) {
        // For now just update position instantly
        // Could be enhanced with smooth animation later
        updatePawnPosition(pawnIndex, newPosition);
    }


    private void handleInput(float delta) {
        // Camera rotation with arrow keys
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            cameraRotation += 90 * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            cameraRotation -= 90 * delta;
        }

        // Camera zoom with up/down arrows
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            cameraDistance = Math.max(8, cameraDistance - 5 * delta);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            cameraDistance = Math.min(20, cameraDistance + 5 * delta);
        }

        // Handle pawn selection on click
        if (Gdx.input.justTouched() && canMove) {
            handlePawnSelection(Gdx.input.getX(), Gdx.input.getY());
        }
    }

    private void handlePawnSelection(int screenX, int screenY) {
        if (currentPlayer != null && currentPlayer.getName().equals(game.getGameStateManager().getCurrentUsername())) {
            int selectedPawn = renderer.getPawnAtScreenCoords(screenX, screenY, camera);
            if (selectedPawn != -1) {
                game.getGameStateManager().requestMove(selectedPawn);
                canMove = false;
            }
        }
    }


    public void updatePawnPosition(int pawnIndex, int newPosition) {
        renderer.updatePawnPosition(pawnIndex, newPosition);
    }
}
