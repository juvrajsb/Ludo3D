package ludo.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import ludo.client.GameStateManager;
import ludo.client.LudoGame;
import ludo.client.handlers.*;
import ludo.client.render.*;
import ludo.client.ui.*;
import ludo.core.entities.*;
import java.util.List;
import java.util.Map;



public class GameScreen implements Screen {
    private final LudoGame game;
    private final Stage stage;
    private final GameCamera camera;
    private final BoardRenderer boardRenderer;
    private final DiceRenderer diceRenderer;
    private final PawnRenderer pawnRenderer;
    private final GameHUD gameHUD;
//    private final GameEventHandler gameEventHandler;
//    private final NetworkEventHandler networkHandler;
    private final GameStateManager gameStateManager;

    private Player currentPlayer;
    private boolean isMyTurn;
    private int lastDiceRoll;
    private boolean canMove;

    public GameScreen(LudoGame game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
        this.camera = new GameCamera();
        this.boardRenderer = new BoardRenderer();
        this.diceRenderer = new DiceRenderer();
        this.pawnRenderer = new PawnRenderer();
        this.gameHUD = new GameHUD();
//        this.gameEventHandler = new GameEventHandler(this);
//        this.networkHandler = new NetworkEventHandler(game.getClient());

        this.gameStateManager = new GameStateManager();
        this.gameStateManager.initialize(this);

        setupUI();
        setupInputHandling();
    }

    private void setupUI() {
        stage.addActor(gameHUD);
        Gdx.input.setInputProcessor(stage);
    }

    private void setupInputHandling() {
        // Set up input handling for dice rolls and pawn movement
        stage.addListener(event -> {
            if (event instanceof InputEvent inputEvent) {
                if (inputEvent.getType() == InputEvent.Type.touchDown) {
                    handleInput(inputEvent.getStageX(), inputEvent.getStageY());
                }
            }
            return false;
        });

        // Initialize game state manager
        gameStateManager.initialize(this);
    }

    private void handleInput(float x, float y) {
        if (!canMove) {
            // Handle dice roll
            if (diceRenderer.isClicked(x, y)) {
                gameStateManager.requestDiceRoll();
            }
        } else {
            // Handle pawn selection
            int pawnIndex = pawnRenderer.getPawnAtPosition(x, y); //TODO check
            if (pawnIndex != -1) {
                gameStateManager.requestMove(pawnIndex);
            }
        }
    }

    @Override
    public void render(float delta) {
        // Clear screen
        Gdx.gl.glClearColor(0.8f, 0.8f, 0.8f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();

        // Render game elements
        boardRenderer.render(game.getPlayers().toArray(new Player[0])); //TODO check

        for (Player player : game.getPlayers()) {
            pawnRenderer.render(player);
        }
        diceRenderer.render(lastDiceRoll, 700, 100);

        // Update and render UI
        stage.act(delta);
        stage.draw();
    }

    // Methods called by handlers to update game state
    public void updateDiceDisplay(int value) {
        lastDiceRoll = value;
        diceRenderer.updateValue(value);
        canMove = true;
        gameHUD.updateDiceRoll(value);
    }

    public void enablePawnSelection() {
        canMove = true;
        gameHUD.showMessage("Select a pawn to move");
    }

    public void updatePawnPosition(int pawnIndex, int newPosition) {
        if (currentPlayer != null) {
            currentPlayer.getPawns().get(pawnIndex).setPosition(newPosition);
        }
        canMove = false;
    }

    public void updatePlayerPawns(String color, List<Integer> positions) {
        for (Player player : game.getPlayers()) {
            if (player.getColor().equals(color)) {
                for (int i = 0; i < positions.size(); i++) {
                    player.getPawns().get(i).setPosition(positions.get(i));
                }
                break;
            }
        }
    }

    public void setCurrentPlayer(String playerColor) {
        for (Player player : game.getPlayers()) {
            if (player.getColor().equals(playerColor)) {
                currentPlayer = player;
                isMyTurn = player.getName().equals(game.getCurrentPlayerName());
                gameHUD.updateCurrentPlayer(player.getName());
                break;
            }
        }
    }

    public void updateGameState(String state) {
        gameHUD.updateGameState(state);
    }

    public void showMessage(String message) {
        gameHUD.showMessage(message);
    }

    public void addPlayer(Player player) {
        game.addPlayer(player);
        gameHUD.updatePlayers(game.getPlayers());
    }

    public void enableControls() {
        isMyTurn = true;
        canMove = false;
        gameHUD.enableControls();
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void playMoveAnimation(int pawnIndex, int newPosition) {
        // Implement pawn movement animation here
        // This could use tweening or other animation techniques
    }

    public void showWinnerScreen(String winner) {
        gameHUD.showWinnerScreen(winner);
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void hide() {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {
        stage.dispose();
        boardRenderer.dispose();
        diceRenderer.dispose();
        pawnRenderer.dispose();
        gameStateManager.dispose();
    }

    public void disableControls() {
    }

    public void updateWaitingRoom() {
    }

    public void removePlayer(String playerName) {
    }

    public void showWaitingRoom() {
    }

    public void showError(String message) {

    }
}

