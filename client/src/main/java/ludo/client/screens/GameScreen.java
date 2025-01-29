package ludo.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import ludo.client.GameStateManager;
import ludo.client.LudoGame;
import ludo.client.assets.GameAssets;
import ludo.client.render.GameRenderer;
import ludo.client.ui.GameHUD;
import ludo.core.entities.*;

import java.util.List;

public class GameScreen extends BaseScreen implements GameView {
    private final GameHUD hud;
    private final GameRenderer renderer;
    private final GameStateManager gameManager;

    // Game state
    private Player currentPlayer;
    private int selectedPawnIndex = -1;
    private boolean canMove;
    private int lastDiceRoll;

    public GameScreen(LudoGame game) {
        super(game);
        this.hud = new GameHUD();
        this.renderer = new GameRenderer(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        this.gameManager = new GameStateManager();

        setupUI();
        initializeGame();
    }

    private void setupUI() {
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.add(hud).expand().fill();
        stage.addActor(mainTable);
    }

    private void initializeGame() {
        gameManager.initialize(this);
        renderer.createPawnModels(
            GameAssets.getInstance().getPawnModel(),
            ((LudoGame)game).getPlayers().toArray(new Player[0])
        );
    }

    @Override
    public void render(float delta) {
        handleInput(delta);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        renderer.render();
        stage.act(delta);
        stage.draw();
    }

    // GameView interface implementation
    @Override
    public void updateGameState(String state) {
        hud.updateGameState(state);
    }

    @Override
    public void setCurrentPlayer(String playerColor) {
        for (Player player : ((LudoGame)game).getPlayers()) {
            if (player.getColor().equals(playerColor)) {
                currentPlayer = player;
                hud.updateCurrentPlayer(player.getName());
                break;
            }
        }
    }

    @Override
    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    @Override
    public void updatePawnPosition(int pawnIndex, int newPosition) {
        if (currentPlayer != null) {
            currentPlayer.getPawns().get(pawnIndex).setPosition(newPosition);
            renderer.updatePawnPosition(
                getGlobalPawnIndex(currentPlayer, pawnIndex),
                newPosition
            );
        }
    }

    @Override
    public void updatePlayerPawns(String color, List<Integer> positions) {
        for (Player player : ((LudoGame)game).getPlayers()) {
            if (player.getColor().equals(color)) {
                for (int i = 0; i < positions.size(); i++) {
                    player.getPawns().get(i).setPosition(positions.get(i));
                    renderer.updatePawnPosition(
                        getGlobalPawnIndex(player, i),
                        positions.get(i)
                    );
                }
                break;
            }
        }
    }

    @Override
    public void enablePawnSelection() {
        canMove = true;
        showMessage("Select a pawn to move");
    }

    @Override
    public void playMoveAnimation(int pawnIndex, int newPosition) {
        // For now, just update position directly
        updatePawnPosition(pawnIndex, newPosition);
        // TODO: Add smooth animation
    }

    @Override
    public void addPlayer(Player player) {
        ((LudoGame)game).addPlayer(player);
        hud.updatePlayers(((LudoGame)game).getPlayers());
        renderer.createPawnModels(
            GameAssets.getInstance().getPawnModel(),
            ((LudoGame)game).getPlayers().toArray(new Player[0])
        );
    }

    @Override
    public void removePlayer(String playerName) {
        ((LudoGame)game).getPlayers().removeIf(p -> p.getName().equals(playerName));
        hud.updatePlayers(((LudoGame)game).getPlayers());
    }

    @Override
    public void updateDiceDisplay(int value) {
        lastDiceRoll = value;
        hud.updateDiceRoll(value);
    }

    @Override
    public void enableControls() {
        canMove = true;
        hud.enableControls();
    }

    @Override
    public void disableControls() {
        canMove = false;
        hud.disableControls();
    }

    @Override
    public void showMessage(String message) {
        hud.showMessage(message);
    }

    @Override
    public void showError(String message) {
        hud.showMessage("Error: " + message);
    }

    @Override
    public void showWinnerScreen(String winner) {
        hud.showWinnerScreen(winner);
    }

    @Override
    public void showWaitingRoom() {
        // TODO: Implement waiting room visualization
    }

    @Override
    public void updateWaitingRoom() {
        // TODO: Update waiting room state
    }

    private void handleInput(float delta) {
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            renderer.rotateCamera(delta * 2);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            renderer.rotateCamera(-delta * 2);
        }

        if (Gdx.input.justTouched() && canMove) {
            handlePawnSelection(Gdx.input.getX(), Gdx.input.getY());
        }
    }

    private void handlePawnSelection(int screenX, int screenY) {
        // TODO: Implement ray casting for pawn selection
        if (selectedPawnIndex != -1 && currentPlayer != null) {
            gameManager.requestMove(selectedPawnIndex);
        }
    }

    private int getGlobalPawnIndex(Player player, int localIndex) {
        int playerIndex = ((LudoGame)game).getPlayers().indexOf(player);
        return playerIndex * 4 + localIndex;
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        renderer.resize(width, height);
    }

    @Override
    public void dispose() {
        super.dispose();
        renderer.dispose();
    }
}

//private void handlePawnSelection(int screenX, int screenY) {
//    if (!canMove || currentPlayer == null) {
//        return;
//    }
//
//    // TODO: Implement proper ray casting for pawn selection
//    // For now, we'll use a simple selection method
//    for (int i = 0; i < currentPlayer.getPawns().size(); i++) {
//        // Check if pawn is clicked (simplified)
//        Pawn pawn = currentPlayer.getPawns().get(i);
//        if (isClickNearPawn(screenX, screenY, pawn.getPosition())) {
//            gameStateManager.requestMove(i);
//            canMove = false;
//            break;
//        }
//    }
//}
//
//private boolean isClickNearPawn(int screenX, int screenY, int pawnPosition) {
//    // Convert screen coordinates to world coordinates
//    // This is a simplified version - you'll need to implement proper 3D picking
//    // using ray casting with the camera
//
//    // For now, return true if click is near where we think the pawn should be
//    float x = (pawnPosition % 10) - 5f;
//    float z = (pawnPosition / 10) - 5f;
//
//    // Convert world coordinates to screen coordinates
//    // This is very simplified and needs proper implementation
//    return true; // TODO: Implement proper click detection
//}
