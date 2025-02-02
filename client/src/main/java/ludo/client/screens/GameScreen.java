package ludo.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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

import java.util.List;

public class GameScreen extends BaseScreen {
    private final GameHUD hud;
    private final GameRenderer renderer;
    private final ModelBatch modelBatch;
    private final Environment environment;
    private final PerspectiveCamera camera;
    private final TextButton rollButton;
    private float cameraRotation = 0;
    private float cameraDistance = 12f;
    private Vector3 cameraTarget = new Vector3(0, 0, 0);

    // Game state
    private Player currentPlayer;
    private int selectedPawnIndex = -1;
    private boolean canMove;
    private int lastDiceRoll;
    private boolean isRolling = false;

    public GameScreen(LudoGame game) {
        super(game);

        // Initialize 3D rendering components
        modelBatch = new ModelBatch();
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        // Set up camera
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        updateCameraPosition();
        camera.lookAt(0, 0, 0);
        camera.near = 1f;
        camera.far = 300f;
        camera.update();

        // Initialize UI components
        this.hud = new GameHUD();

        // Create roll button
        rollButton = new TextButton("Roll Dice", skin);
        rollButton.setSize(100, 50);
        rollButton.setPosition(Gdx.graphics.getWidth() - 120, 20);
        rollButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!isRolling && game.getGameStateManager().getCurrentUsername().equals(currentPlayer.getName())) {
                    requestDiceRoll();
                }
            }
        });
        stage.addActor(rollButton);

        // Initialize renderer
        this.renderer = new GameRenderer(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Load initial game state
        initializeGame();
    }

    private void initializeGame() {
//        GameAssets.getInstance().loadModels();
        renderer.createPawns(game.getPlayers());
        updateGameState();
    }

    public void updateGameState() {
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

    public void updatePlayerPawns(String color, List<Integer> positions) {
        for (Player player : game.getPlayers()) {
            if (player.getColor().equals(color)) {
                for (int i = 0; i < positions.size(); i++) {
                    renderer.updatePawnPosition(player.getPawns().get(i).getPosition(), positions.get(i));
                }
                break;
            }
        }
    }

    public void addPlayer(Player player) {
        game.addPlayer(player);
        renderer.createPawns(game.getPlayers());
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

    @Override
    public void render(float delta) {
        handleInput(delta);

        // Clear screen
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.3f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // Update camera
        updateCameraPosition();
        camera.update();

        // Render 3D scene
        modelBatch.begin(camera);
        renderer.render(modelBatch, environment);
        modelBatch.end();

        // Render UI
        stage.act(delta);
        stage.draw();
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

    private void updateCameraPosition() {
        float x = (float)(cameraDistance * Math.cos(Math.toRadians(cameraRotation)));
        float z = (float)(cameraDistance * Math.sin(Math.toRadians(cameraRotation)));
        camera.position.set(x, cameraDistance * 0.7f, z);
        camera.lookAt(cameraTarget);
        camera.up.set(Vector3.Y);
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

    public void updatePawnPosition(int pawnIndex, int newPosition) {
        renderer.updatePawnPosition(pawnIndex, newPosition);
    }

    public void setCurrentPlayer(String playerColor) {
        for (Player player : game.getPlayers()) {
            if (player.getColor().equals(playerColor)) {
                currentPlayer = player;
                hud.updateCurrentPlayer(player.getName());
                rollButton.setDisabled(!player.getName().equals(game.getGameStateManager().getCurrentUsername()));
                break;
            }
        }
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
}
