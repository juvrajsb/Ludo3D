package ludo.client.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import ludo.client.assets.GameAssets;
import ludo.core.entities.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ludo.core.entities.Pawn;
public class GameRenderer {
    private static final String TAG = "GameRenderer";
    private Model boardModel;
    private ModelInstance boardInstance;
    private Array<ModelInstance> pawnInstances;
    private Map<Integer, Vector3> pawnPositions;
    private Map<Integer, Boolean> highlightedPawns = new HashMap<>();
    private static final Color HIGHLIGHT_COLOR = new Color(1, 1, 0, 0.5f);
    private final Vector3 tempVector = new Vector3();
    private Map<String, Color> playerColors;
    private final float BOARD_SIZE = 8f;
    private final float PAWN_SCALE = 2f;
    private final Ray ray;
    private final Vector3 intersection;
    private Texture boardTexture;
    private final float SQUARE_SIZE = BOARD_SIZE / 15f; // 15x15 grid
    private DiceRenderer diceRenderer;

    public GameRenderer(int width, int height) {
        Gdx.app.log(TAG, "Initializing GameRenderer");
        pawnInstances = new Array<>();
        pawnPositions = new HashMap<>();
        ray = new Ray();
        intersection = new Vector3();
        diceRenderer = new DiceRenderer();
        initializePlayerColors();
        createBoard();
    }

    public int getNumberOfPawns() {
        return pawnInstances.size;
    }

    public void updateDiceValue(int value) {
        diceRenderer.startRoll(value);
    }

    public void render(ModelBatch modelBatch, Environment environment) {
        // Render board and pawns
        if (boardInstance != null) {
            modelBatch.render(boardInstance, environment);
        }

        for (ModelInstance pawn : pawnInstances) {
            modelBatch.render(pawn, environment);
        }

        // Update and render dice
        diceRenderer.update(Gdx.graphics.getDeltaTime());
        diceRenderer.render(modelBatch);
    }

    private void createBoard() {
        try {
            // Load board texture
            boardTexture = new Texture(Gdx.files.internal("images/board.png"));
            boardTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

            // Create material with texture
            Material material = new Material(TextureAttribute.createDiffuse(boardTexture));

            // Create board model
            ModelBuilder modelBuilder = new ModelBuilder();
            boardModel = modelBuilder.createBox(
                BOARD_SIZE,
                0.2f, // height/thickness of board
                BOARD_SIZE,
                material,
                Usage.Position | Usage.Normal | Usage.TextureCoordinates
            );

            // Create board instance
            boardInstance = new ModelInstance(boardModel);
            boardInstance.transform.translate(0, -0.1f, 0); // Slightly below pawns
            Gdx.app.log(TAG, "Board created successfully");
        } catch (Exception e) {
            Gdx.app.error(TAG, "Error creating board: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Vector3 calculatePawnPosition(int boardPosition, String playerColor) {
        float BOARD_SCALE = 8f;  // Board size in world units
        float CELL_SIZE = BOARD_SCALE / 15f;  // Size of each cell (15x15 grid)
        float BOARD_HEIGHT = 0.1f;  // Height of the board
        float PAWN_HEIGHT = 0.2f;   // Height to place pawn above board
        float halfBoard = BOARD_SCALE / 2;
        float cornerOffset = (BOARD_SCALE / 2) - CELL_SIZE;

        if (boardPosition == -1) {
            // Home positions based on color
            Vector3 homePosition = new Vector3();
            homePosition.y = BOARD_HEIGHT + PAWN_HEIGHT;  // Common Y position for all pawns

            switch (playerColor.toUpperCase()) {
                case "RED":
                    homePosition.x = cornerOffset - 1.0f;  // Offset to place in red corner
                    homePosition.z = cornerOffset - 1.0f;
                    break;
                case "GREEN":
                    homePosition.x = -cornerOffset + 1.0f;
                    homePosition.z = cornerOffset - 1.0f;
                    break;
                case "BLUE":
                    homePosition.x = cornerOffset - 1.0f;
                    homePosition.z = -cornerOffset + 1.0f;
                    break;
                case "YELLOW":
                    homePosition.x = -cornerOffset + 1.0f;
                    homePosition.z = -cornerOffset + 1.0f;
                    break;
            }
            return homePosition;
        }

        // Calculate position on board
        int row = boardPosition / 15;
        int col = boardPosition % 15;

        float x = (col * CELL_SIZE) - halfBoard;
        float z = (row * CELL_SIZE) - halfBoard;
        float y = BOARD_HEIGHT + PAWN_HEIGHT;

        return new Vector3(x, y, z);
    }

    private void positionPawn(ModelInstance pawnInstance, int boardPosition, int pawnIndex, String playerColor) {
        Vector3 position = calculatePawnPosition(boardPosition, playerColor);

        // Add small offset for multiple pawns in same position
        float offset = 0.2f * (pawnIndex % 4);  // Reduced offset
        position.x += offset;
        position.z += offset;

        pawnInstance.transform.setToTranslation(position);
        pawnInstance.transform.scale(PAWN_SCALE, PAWN_SCALE, PAWN_SCALE);

        // Store position for reference
        pawnPositions.put(pawnIndex, position);
    }

    public void createPawns(List<Player> players) {
        Gdx.app.log(TAG, "Creating pawns for " + players.size() + " players");
        pawnInstances.clear();
        pawnPositions.clear();

        Model pawnModel = GameAssets.getInstance().getPawnModel();
        if (pawnModel == null) {
            Gdx.app.error(TAG, "Pawn model is null!");
            return;
        }

        int pawnIndex = 0;
        for (Player player : players) {
            Gdx.app.log(TAG, "Processing player: " + player.getName() +
                " Color: " + player.getColor() +
                " Pawns: " + player.getPawns().size());

            Color playerColor = playerColors.get(player.getColor());
            Material pawnMaterial = new Material(ColorAttribute.createDiffuse(playerColor));

            int playerPawnCount = 0;
            for (Pawn pawn : player.getPawns()) {
                ModelInstance pawnInstance = new ModelInstance(pawnModel);
                pawnInstance.materials.get(0).set(pawnMaterial);

                int position = pawn.getPosition();
                // Pass both absolute pawnIndex and player-specific pawnCount
                positionPawn(pawnInstance, position, playerPawnCount, player.getColor());

                pawnInstances.add(pawnInstance);
                Gdx.app.log(TAG, "Created pawn " + pawnIndex + " at position " + position +
                    " for player " + player.getColor());

                pawnIndex++;
                playerPawnCount++;
            }
        }
        Gdx.app.log(TAG, "Finished creating pawns. Total: " + pawnInstances.size);
    }

    public void highlightSelectablePawns(Player player, int diceRoll) {
        highlightedPawns.clear();
        int playerStartIdx = player.getPawns().get(0).getPosition();

        for (int i = 0; i < player.getPawns().size(); i++) {
            Pawn pawn = player.getPawns().get(i);
            boolean canMove = false;

            if (pawn.isHome() && diceRoll == 6) {
                canMove = true;
            } else if (!pawn.isHome() && !pawn.isFinished()) {
                // Add logic to check if move is valid
                canMove = true;
            }

            if (canMove) {
                int pawnIdx = playerStartIdx + i;
                highlightedPawns.put(pawnIdx, true);
                ModelInstance pawnInstance = pawnInstances.get(pawnIdx);
                // Add glow effect or outline
                Material mat = pawnInstance.materials.get(0);
                mat.set(ColorAttribute.createDiffuse(HIGHLIGHT_COLOR));
            }
        }
    }

    public void clearHighlights() {
        for (Map.Entry<Integer, Boolean> entry : highlightedPawns.entrySet()) {
            int pawnIdx = entry.getKey();
            ModelInstance pawnInstance = pawnInstances.get(pawnIdx);
            // Reset material to original color
            Material mat = pawnInstance.materials.get(0);
            mat.set(ColorAttribute.createDiffuse(
                playerColors.get(pawnInstance.userData.toString())));
        }
        highlightedPawns.clear();
    }

    public boolean isPawnHighlighted(int pawnIndex) {
        return highlightedPawns.containsKey(pawnIndex);
    }

//    public void updatePawnPosition(int pawnIndex, int newPosition) {
//        if (pawnIndex >= 0 && pawnIndex < pawnInstances.size) {
//            ModelInstance pawn = pawnInstances.get(pawnIndex);
//            Vector3 newPos = calculatePawnPosition(newPosition);
//
//            // Update transform and stored position
//            pawn.transform.setToTranslation(newPos);
//            pawn.transform.scale(PAWN_SCALE, PAWN_SCALE, PAWN_SCALE);
//            pawnPositions.put(pawnIndex, newPos);
//        }
//    }

//    private String findPlayerColorForPawnIndex(int pawnIndex) {
//        // Each player has 4 pawns, so we can determine the player by dividing the pawn index by 4
//        int playerIndex = pawnIndex / 4;
//        String[] colors = {"RED", "GREEN", "BLUE", "YELLOW"};
//        if (playerIndex < colors.length) {
//            return colors[playerIndex];
//        }
//        return null;
//    }

    public void updatePawnPosition(int pawnIndex, int newPosition, String playerColor) {
        if (pawnIndex >= 0 && pawnIndex < pawnInstances.size) {
            ModelInstance pawn = pawnInstances.get(pawnIndex);
            Vector3 newPos = calculatePawnPosition(newPosition, playerColor);

            // Update transform and stored position
            pawn.transform.setToTranslation(newPos);
            pawn.transform.scale(PAWN_SCALE, PAWN_SCALE, PAWN_SCALE);
            pawnPositions.put(pawnIndex, newPos);
        }
    }

    public void updateAllPawnPositions() {
        for (int i = 0; i < pawnInstances.size; i++) {
            ModelInstance pawn = pawnInstances.get(i);
            Vector3 position = pawnPositions.get(i);
            if (position != null) {
                pawn.transform.setToTranslation(position);
                pawn.transform.scale(PAWN_SCALE, PAWN_SCALE, PAWN_SCALE);
            }
        }
    }

    public void dispose() {
        if (boardModel != null) boardModel.dispose();
        if (boardTexture != null) boardTexture.dispose();
        if (diceRenderer != null) diceRenderer.dispose();
        Gdx.app.log(TAG, "GameRenderer disposed");
    }

    private void initializePlayerColors() {
        playerColors = new HashMap<>();
        playerColors.put("RED", Color.RED);
        playerColors.put("GREEN", Color.GREEN);
        playerColors.put("BLUE", Color.BLUE);
        playerColors.put("YELLOW", Color.YELLOW);
    }

    public int getPawnAtScreenCoords(int screenX, int screenY, Camera camera) {
        Ray ray = camera.getPickRay(screenX, screenY);
        float minDist = Float.MAX_VALUE;
        int selectedPawn = -1;

        // Larger selection radius for easier clicking
        float selectionRadius = 0.5f;  // Increased radius

        for (int i = 0; i < pawnInstances.size; i++) {
            Vector3 pawnPos = pawnPositions.get(i);
            if (pawnPos != null) {
                // Create a sphere around the pawn for selection
                if (Intersector.intersectRaySphere(ray, pawnPos, selectionRadius, intersection)) {
                    float dist = intersection.dst2(camera.position);
                    if (dist < minDist) {
                        minDist = dist;
                        selectedPawn = i;
                    }
                }
            }
        }

        // If we found a pawn, provide visual feedback
        if (selectedPawn != -1) {
            highlightPawn(selectedPawn);
            Gdx.app.log("GameRenderer", "Selected pawn: " + selectedPawn);
        }

        return selectedPawn;
    }

    private void highlightPawn(int pawnIndex) {
        ModelInstance pawn = pawnInstances.get(pawnIndex);
        // Add temporary highlight effect
        Material mat = pawn.materials.get(0);
        mat.set(ColorAttribute.createDiffuse(Color.YELLOW));

        // Reset highlight after a short delay
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                String color = (String) pawn.userData;
                mat.set(ColorAttribute.createDiffuse(playerColors.get(color)));
            }
        }, 0.2f);  // Reset after 0.2 seconds
    }

//    public void highlightPawn(int pawnIndex) {
//        if (pawnIndex >= 0 && pawnIndex < pawnInstances.size) {
//            ModelInstance pawn = pawnInstances.get(pawnIndex);
//            // Add highlight effect (e.g., glow or outline)
//            // This would require additional shader implementation
//        }
//    }

//    public void updatePawnPositions() {
//        // Update all pawn positions based on current state
//        for (int i = 0; i < pawnInstances.size; i++) {
//            ModelInstance pawn = pawnInstances.get(i);
//            Vector3 position = pawnPositions.get(i);
//            if (position != null) {
//                pawn.transform.setToTranslation(position);
//                pawn.transform.scale(PAWN_SCALE, PAWN_SCALE, PAWN_SCALE);
//            }
//        }
//    }

//    public void resize(int width, int height) {
//        // Update any viewport-dependent calculations if needed
//    }
}
