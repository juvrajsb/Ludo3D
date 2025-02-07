package ludo.client.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;

import ludo.client.assets.GameAssets;
import ludo.core.entities.Board;
import ludo.core.entities.Cell;
import ludo.core.entities.Pawn;
import ludo.core.entities.Player;
import ludo.core.utils.BoardCoordinates;

import java.util.*;
import java.util.logging.Logger;

public class GameRenderer {
    private static final Logger LOGGER = Logger.getLogger(GameRenderer.class.getName());
    private static final String TAG = "GameRenderer";
    private final float BOARD_SIZE = 15f;
    private final float PAWN_SCALE = 5f;
    private final float BOARD_SCALE = 0.5f;
    private final float BOARD_OFFSET = BOARD_SIZE/2;
    private final Vector3 intersection;
    private final Board gameBoard;
    private final Array<ModelInstance> pawnInstances;
    private final Map<Integer, Vector3> pawnPositions;
    private final Map<Integer, Boolean> highlightedPawns = new HashMap<>();
    private final DiceRenderer diceRenderer;
    private final Map<Integer, PawnAnimation> pawnAnimations;
    private final Map<String, Integer> pawnIndicesInBase = new HashMap<>();
    private Model boardModel;
    private ModelInstance boardInstance;
    private Map<String, Color> playerColors;
    private Texture boardTexture;
    private String currentPlayerColor;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private SpriteBatch spriteBatch;
    private static final float PAWN_RADIUS = 0.25f;  // Adjust this value based on your pawn size


    public GameRenderer(int width, int height) {
        pawnInstances = new Array<>();
        pawnPositions = new HashMap<>();
        pawnAnimations = new HashMap<>();
        gameBoard = new Board();
        intersection = new Vector3();
        diceRenderer = new DiceRenderer();
        spriteBatch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        font.getData().setScale(0.01f);
        initializePlayerColors();
        createBoard();
    }

    public void updateDiceValue(int value) {
        diceRenderer.startRoll(value);
    }

    private void renderDebugGrid() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);

        // Draw grid lines and coordinates
        for (int i = 0; i <= BOARD_SIZE; i++) {
            Vector3 startVert = gridToWorld(i, 0);
            Vector3 endVert = gridToWorld(i, BOARD_SIZE);
            Vector3 startHoriz = gridToWorld(0, i);
            Vector3 endHoriz = gridToWorld(BOARD_SIZE, i);

            shapeRenderer.line(startVert.x, startVert.z, endVert.x, endVert.z);
            shapeRenderer.line(startHoriz.x, startHoriz.z, endHoriz.x, endHoriz.z);

            // Log every 5th line position
            if (i % 5 == 0) {
                Gdx.app.log(TAG, String.format("Grid line %d: vertical(%.1f, %.1f) -> (%.1f, %.1f)",
                    i, startVert.x, startVert.z, endVert.x, endVert.z));
            }
        }
        shapeRenderer.end();
    }

    public void render(ModelBatch modelBatch, Environment environment, float delta) {
        updateAnimations(delta);

        if (boardInstance != null) {
            modelBatch.render(boardInstance, environment);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.G)) {  // Press G to toggle grid
            renderDebugGrid();
        }

        for (ModelInstance pawn : pawnInstances) {
            if (pawn != null) {
                modelBatch.render(pawn, environment);
            }
        }

        diceRenderer.update(delta);
        diceRenderer.render(modelBatch);
    }

    public void createPawns(List<Player> players) {
        Gdx.app.log(TAG, String.format("Creating pawns for %d players", players.size()));
        pawnInstances.clear();
        pawnPositions.clear();

        // Prealloca una lista di dimensione 16 con null
        for (int i = 0; i < 16; i++) {
            pawnInstances.add(null);
        }

        Model pawnModel = GameAssets.getInstance().getPawnModel();
        if (pawnModel == null) {
            Gdx.app.error(TAG, "Pawn model is null!");
            return;
        }

        for (Player player : players) {
            Gdx.app.log(TAG, "Processing player: " + player.getName() +
                " Color: " + player.getColor() +
                " Pawns: " + player.getPawns().size());

            // Get the color for this player's pawns
            Color playerColor = getPlayerColor(player.getColor());

            // Create material with diffuse color attribute
            Material pawnMaterial = new Material(
                ColorAttribute.createDiffuse(playerColor),
                ColorAttribute.createSpecular(1, 1, 1, 1),  // White specular highlights
                ColorAttribute.createAmbient(playerColor.r * 0.5f,
                    playerColor.g * 0.5f,
                    playerColor.b * 0.5f,
                    1f)  // Darker ambient color
            );

            for (int i = 0; i < player.getPawns().size(); i++) {
                Pawn pawn = player.getPawns().get(i);
                ModelInstance pawnInstance = new ModelInstance(pawnModel);

                // Apply the material to all parts of the model
                for (Material mat : pawnInstance.materials) {
                    mat.clear();
                    mat.set(pawnMaterial);
                }

                int position = pawn.getPosition();
                int globalPawnIndex = getGlobalPawnIndex(player.getColor(), i);
                positionPawn(pawnInstance, position, globalPawnIndex, player.getColor());

                pawnInstances.set(globalPawnIndex, pawnInstance);
                Gdx.app.log(TAG, "Created pawn " + globalPawnIndex + " at position " + position +
                    " for player " + player.getColor());
            }
        }

        Gdx.app.log(TAG, String.format("Finished creating pawns. Total: %d", pawnInstances.size));
    }

    public void updateAnimations(float delta) {
        Iterator<Map.Entry<Integer, PawnAnimation>> it = pawnAnimations.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, PawnAnimation> entry = it.next();
            PawnAnimation anim = entry.getValue();
            if (anim.update(delta)) {
                // Animation complete, remove it
                it.remove();
            } else {
                // Update pawn position
                int pawnIndex = entry.getKey();
                ModelInstance pawn = pawnInstances.get(pawnIndex);
                Vector3 pos = anim.getCurrentPosition();
                pawn.transform.setTranslation(pos);
            }
        }
    }

    private void createBoard() {
        try {
            // Load the board texture
            boardTexture = new Texture(Gdx.files.internal("images/board.png"));
            boardTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

            // Create the board model with cells
            ModelBuilder modelBuilder = new ModelBuilder();
            Material material = new Material(TextureAttribute.createDiffuse(boardTexture));

            // Create a grid of cells
            boardModel = modelBuilder.createBox(
                BOARD_SIZE,
                0.2f, // height/thickness of board
                BOARD_SIZE,
                material,
                Usage.Position | Usage.Normal | Usage.TextureCoordinates
            );

            boardInstance = new ModelInstance(boardModel);
            boardInstance.transform.translate(0, 0, 0);

            // Log board corners in world coordinates
            logBoardCorners();
        } catch (Exception e) {
            Gdx.app.error(TAG, "Error creating board: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void logBoardCorners() {
        float halfSize = BOARD_SIZE / 2;
        Vector3 topLeft = new Vector3(-halfSize, 0, halfSize);
        Vector3 topRight = new Vector3(halfSize, 0, halfSize);
        Vector3 bottomLeft = new Vector3(-halfSize, 0, -halfSize);
        Vector3 bottomRight = new Vector3(halfSize, 0, -halfSize);

        Gdx.app.log(TAG, "Board corners in world coordinates:");
        Gdx.app.log(TAG, String.format("Top Left: %s", topLeft));
        Gdx.app.log(TAG, String.format("Top Right: %s", topRight));
        Gdx.app.log(TAG, String.format("Bottom Left: %s", bottomLeft));
        Gdx.app.log(TAG, String.format("Bottom Right: %s", bottomRight));
    }

//    public void updatePawnPosition(int pawnIndex, int newPosition, String playerColor) {
//        if (pawnIndex >= 0 && pawnIndex < pawnInstances.size) {
//            Vector3 currentPos = pawnInstances.get(pawnIndex).transform.getTranslation(new Vector3());
//            Vector3 targetPos = calculatePawnPosition(newPosition, playerColor, pawnIndex % 4);
//
//            // Only create animation if positions are different
//            if (!currentPos.epsilonEquals(targetPos, 0.001f)) {
//                Gdx.app.log("GameRenderer", String.format(
//                    "Updating pawn %d (%s) position: %d -> Current(%s), Target(%s)",
//                    pawnIndex, playerColor, newPosition,
//                    currentPos.toString(),
//                    targetPos.toString()));
//
//                PawnAnimation animation = new PawnAnimation(currentPos.cpy(), targetPos.cpy(), playerColor);
//                pawnAnimations.put(pawnIndex, animation);
//                pawnPositions.put(pawnIndex, targetPos.cpy());
//            }
    private Vector3 calculatePawnPosition(int boardPosition) {
        Cell cell = gameBoard.getGridPosition(boardPosition);
        if (cell == null) {
            Gdx.app.error(TAG, "Invalid board position: " + boardPosition);
            return new Vector3(0, 0.2f, 0);
        }

        int gridX = cell.getX();
        int gridZ = cell.getY();

        float worldX = (gridX - (BOARD_SIZE / 2)) * BOARD_SCALE;
        float worldZ = (BOARD_SIZE / 2 - gridZ) * BOARD_SCALE; // Flip Z axis

        return new Vector3(worldX, 0.2f, worldZ);
    }

//    public void updatePawnPosition(int pawnIndex, int newPosition, String playerColor) {
//        int globalPawnIndex = getGlobalPawnIndex(playerColor, pawnIndex);
//
//        Gdx.app.log(TAG, String.format("[POSITION] Updating %s pawn %d to position %d",
//            playerColor, pawnIndex, newPosition));
//
//        ModelInstance pawn = pawnInstances.get(globalPawnIndex);
//        Vector3 targetPos;
//
//        if (newPosition == -1) {
//            targetPos = calculateHomeBasePosition(playerColor, pawnIndex);
//        } else {
//            targetPos = calculateTargetPosition(playerColor, newPosition);
//        }
//
//        Vector3 currentPos = pawn.transform.getTranslation(new Vector3());
//        PawnAnimation animation = new PawnAnimation(currentPos, targetPos, playerColor);
//        pawnAnimations.put(globalPawnIndex, animation);
//        pawnPositions.put(globalPawnIndex, targetPos);
//    }

//    public void updatePawnPosition(int pawnIndex, int newPosition, String playerColor) {
//        // Calculate global pawn index
//        int colorOffset;
//        switch(playerColor.toUpperCase()) {
//            case "RED": colorOffset = 0; break;
//            case "BLUE": colorOffset = 4; break;
//            case "GREEN": colorOffset = 8; break;
//            case "YELLOW": colorOffset = 12; break;
//            default: return;
//        }
//        int globalPawnIndex = colorOffset + (pawnIndex % 4);
//
//        // Ensure we have enough pawns created
//        while (pawnInstances.size <= globalPawnIndex) {
//            ModelInstance newPawn = createPawnInstance(playerColor);
//            pawnInstances.add(newPawn);
//        }
//
//        Vector3 targetPos;
//        if (newPosition == -1) {
//            // Home base position
//            targetPos = calculateHomeBasePosition(playerColor, pawnIndex);
//        } else if (newPosition >= gameBoard.getTotalSpaces()) {
//            // Home column position
//            targetPos = calculateHomeColumnPosition(playerColor, newPosition - gameBoard.getTotalSpaces());
//        } else {
//            // Main track position
//            targetPos = calculateMainTrackPosition(newPosition);
//        }
//
//        Vector3 currentPos = null;
//        if (pawnPositions.containsKey(globalPawnIndex)) {
//            currentPos = pawnPositions.get(globalPawnIndex).cpy();
//        } else {
//            currentPos = targetPos.cpy();
//        }
//
//        PawnAnimation animation = new PawnAnimation(currentPos, targetPos, playerColor);
//        pawnAnimations.put(globalPawnIndex, animation);
//        pawnPositions.put(globalPawnIndex, targetPos);
//    }

    private ModelInstance createPawnInstance(String color) {
        Model pawnModel = GameAssets.getInstance().getPawnModel();
        if (pawnModel == null) {
            Gdx.app.error(TAG, "Pawn model is null!");
            return null;
        }

        ModelInstance pawn = new ModelInstance(pawnModel);

        // Set pawn color based on player color
        Color pawnColor = getPlayerColor(color);
        Material pawnMaterial = new Material(
            ColorAttribute.createDiffuse(pawnColor),
            ColorAttribute.createSpecular(1, 1, 1, 1),
            ColorAttribute.createAmbient(pawnColor.r * 0.5f,
                pawnColor.g * 0.5f,
                pawnColor.b * 0.5f,
                1f)
        );

        // Apply the material to all parts of the model
        for (Material mat : pawn.materials) {
            mat.clear();
            mat.set(pawnMaterial);
        }

        // Apply scale
        pawn.transform.scale(PAWN_SCALE, PAWN_SCALE, PAWN_SCALE);

        return pawn;
    }

    private Color getPlayerColor(String colorName) {
        if (playerColors == null) {
            playerColors = new HashMap<>();
            playerColors.put("RED", new Color(0.8f, 0.2f, 0.2f, 1f));
            playerColors.put("GREEN", new Color(0.2f, 0.8f, 0.2f, 1f));
            playerColors.put("BLUE", new Color(0.2f, 0.2f, 0.8f, 1f));
            playerColors.put("YELLOW", new Color(0.8f, 0.8f, 0.2f, 1f));
        }

        String upperColor = colorName.toUpperCase();
        Color color = playerColors.get(upperColor);

        if (color == null) {
            Gdx.app.error(TAG, "No color found for: " + colorName + ". Using fallback color.");
            return Color.WHITE;
        }

        return color;
    }

    private Vector3 calculateMainTrackPosition(int position) {
        // Main track positions based on diagram
        if (position < 13) {
            // Bottom edge moving right
            return gridToWorld(6 + position, 13);
        } else if (position < 26) {
            // Right edge moving up
            return gridToWorld(13, 13 - (position - 13));
        } else if (position < 39) {
            // Top edge moving left
            return gridToWorld(13 - (position - 26), 1);
        } else {
            // Left edge moving down
            return gridToWorld(1, 1 + (position - 39));
        }
    }

    private Vector3 calculateHomeColumnPosition(String color, int offset) {
        switch(color.toUpperCase()) {
            case "RED":
                // Red home column goes up the middle
                return gridToWorld(7, 8 + offset);
            case "BLUE":
                // Blue home column goes left in middle row
                return gridToWorld(8 - offset, 7);
            case "GREEN":
                // Green home column goes right in middle row
                return gridToWorld(6 + offset, 7);
            case "YELLOW":
                // Yellow home column goes down the middle
                return gridToWorld(7, 6 - offset);
            default:
                return new Vector3(0, 0.2f, 0);
        }
    }

    private Vector3 calculateHomeBasePosition(String color, int index) {
        switch(color.toUpperCase()) {
            case "RED": // Bottom right
                return gridToWorld(13 + (index % 2), 13 + (index / 2));
            case "BLUE": // Bottom left
                return gridToWorld(1 + (index % 2), 13 + (index / 2));
            case "GREEN": // Top left
                return gridToWorld(1 + (index % 2), 1 + (index / 2));
            case "YELLOW": // Top right
                return gridToWorld(13 + (index % 2), 1 + (index / 2));
            default:
                return new Vector3(0, 0.2f, 0);
        }
    }

//    private int getGlobalPawnIndex(String playerColor, int localPawnIndex) {
//        // Calculate offset based on player color
//        int colorOffset = 0;
//        switch(playerColor.toUpperCase()) {
//            case "RED": colorOffset = 0; break;
//            case "BLUE": colorOffset = 4; break;
//            case "GREEN": colorOffset = 8; break;
//            case "YELLOW": colorOffset = 12; break;
//        }
//        return colorOffset + localPawnIndex;
//    }

//    private Vector3 calculateHomeBasePosition(String color, int index) {
//        switch(color.toUpperCase()) {
//            case "RED":
//                return gridToWorld(13 - (index % 2), 13 - (index / 2));
//            case "BLUE":
//                return gridToWorld(1 + (index % 2), 1 + (index / 2));
//            case "GREEN":
//                return gridToWorld(13 - (index % 2), 1 + (index / 2));
//            case "YELLOW":
//                return gridToWorld(1 + (index % 2), 13 - (index / 2));
//            default:
//                return new Vector3(0, 0.2f, 0);
//        }
//    }

    @SuppressWarnings("unused") // Used for pawn animation
    private Vector3 calculateStartPosition(String color) {
        Cell startCell = gameBoard.getStartPositionCell(color);  // Use getStartPositionCell instead of getStartPosition
        if (startCell == null) return new Vector3();
        return gridToWorld(startCell.getX(), startCell.getY());  // Use getY() instead of getZ()
    }

//    private Vector3 calculateTargetPosition(String playerColor, int boardPosition) {
//        // Handle pawns in home base (-1)
//        if (boardPosition == -1) {
//            return calculateHomeBasePosition(playerColor, 0);
//        }
//
//        // Handle pawns in home columns (>=52)
//        if (boardPosition >= gameBoard.getTotalSpaces()) {
//            int homeOffset = boardPosition - gameBoard.getTotalSpaces();
//            switch(playerColor.toUpperCase()) {
//                case "RED": // Goes up
//                    return gridToWorld(7, 8 + homeOffset);
//                case "BLUE": // Goes left
//                    return gridToWorld(8 - homeOffset, 7);
//                case "GREEN": // Goes right
//                    return gridToWorld(6 + homeOffset, 7);
//                case "YELLOW": // Goes down
//                    return gridToWorld(7, 6 - homeOffset);
//                default:
//                    return new Vector3(0, 0.2f, 0);
//            }
//        }
//
//        // Get position from board grid
//        Cell cell = gameBoard.getGridPosition(boardPosition);
//        if (cell != null) {
//            return gridToWorld(cell.getX(), cell.getY());
//        }
//
//        Gdx.app.error(TAG, "Invalid board position: " + boardPosition);
//        return new Vector3(0, 0.2f, 0);
//    }

//    private Vector3 calculateHomeBasePosition(String color, int index) {
//        switch(color.toUpperCase()) {
//            case "RED": // Bottom right
//                return gridToWorld(13 + (index % 2), 13 + (index / 2));
//            case "BLUE": // Bottom left
//                return gridToWorld(13 + (index % 2), 0 + (index / 2));
//            case "GREEN": // Top left
//                return gridToWorld(0 + (index % 2), 0 + (index / 2));
//            case "YELLOW": // Top right
//                return gridToWorld(0 + (index % 2), 13 + (index / 2));
//            default:
//                return new Vector3(0, 0.2f, 0);
//        }
//    }

    private Vector3 gridToWorld(int x, int y) {
        // Convert grid coordinates (0-14) to world space
        float worldX = (x - BOARD_SIZE/2) * BOARD_SCALE;
        float worldZ = (BOARD_SIZE/2 - y) * BOARD_SCALE;
        return new Vector3(worldX, 0.2f, worldZ);
    }

    @SuppressWarnings("unused")  // This method will be used for color-based animations
    private int getColorOffset(String playerColor) {
        switch (playerColor) {
            case "BLUE": return 10;
            case "GREEN": return 20;
            case "YELLOW": return 30;
            default: return 0;
        }
    }

    private Vector3 gridToWorld(float gridX, float gridZ) {
        // Board is 15x15, centered at origin
        // Convert from grid (0-15) to world coordinates (-7.5 to 7.5)
        float worldX = (gridX - BOARD_SIZE/2) * BOARD_SCALE;
        float worldZ = (BOARD_SIZE/2 - gridZ) * BOARD_SCALE; // Flip Z axis to maintain correct orientation

        Gdx.app.log(TAG, String.format("[GRID->WORLD] Converting (%.2f,%.2f) to world coordinates (%.2f,%.2f)",
            gridX, gridZ, worldX, worldZ));

        return new Vector3(worldX, 0.2f, worldZ);
    }

    private void positionPawn(ModelInstance pawnInstance, int boardPosition, int pawnIndex, String playerColor) {
        Vector3 position;

        if (boardPosition == -1) {
            position = calculateHomeBasePosition(playerColor, pawnIndex);
        } else {
            Cell cell = gameBoard.getGridPosition(boardPosition);
            if (cell != null) {
                position = gridToWorld(cell.getX(), cell.getY());
            } else {
                LOGGER.warning("Invalid grid position for board position: " + boardPosition);
                return;
            }
        }

        // Log the calculated position
        Gdx.app.log(TAG, String.format("Positioning %s pawn %d at board position %d -> world position %s",
            playerColor, pawnIndex, boardPosition, position.toString()));

        // Apply position and scale
        pawnInstance.transform.setToTranslation(position);
        pawnInstance.transform.scale(PAWN_SCALE, PAWN_SCALE, PAWN_SCALE);
        pawnInstance.userData = playerColor;
        pawnPositions.put(pawnIndex, position);
    }
    public void clearHighlights() {
        for (Map.Entry<Integer, Boolean> entry : highlightedPawns.entrySet()) {
            int pawnIdx = entry.getKey();
            if (pawnIdx < pawnInstances.size) {
                ModelInstance pawnInstance = pawnInstances.get(pawnIdx);
                if (pawnInstance != null && pawnInstance.userData != null) {
                    Material mat = pawnInstance.materials.get(0);
                    mat.set(ColorAttribute.createDiffuse(
                        playerColors.get(pawnInstance.userData.toString())));
                }
            }
        }
        highlightedPawns.clear();
    }

    public boolean isPawnHighlighted(int pawnIndex) {//TODO check usage not used currently
        return highlightedPawns.containsKey(pawnIndex);
    }

    public void updateAllPawnPositions() {
        for (int i = 0; i < pawnInstances.size; i++) {
            ModelInstance pawn = pawnInstances.get(i);
            Vector3 position = pawnPositions.get(i);

            // If position is null, calculate it based on the pawn's initial state
            if (position == null) {
                String playerColor = (String) pawn.userData;
                int localPawnIndex = i % 4;
                position = calculateHomeBasePosition(playerColor, localPawnIndex);
                pawnPositions.put(i, position);
            }

            Gdx.app.log(TAG, "METHOD:[UPDATEALLPAWNPOSITIONS] Updating pawn " + i + " position to " + position);
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
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (font != null) font.dispose();
        if (spriteBatch != null) spriteBatch.dispose();
        Gdx.app.log(TAG, "GameRenderer disposed");
    }

    private void initializePlayerColors() {
        playerColors = new HashMap<>();

        playerColors.put("RED", new Color(0.8f, 0.2f, 0.2f, 1f));
        playerColors.put("GREEN", new Color(0.2f, 0.8f, 0.2f, 1f));
        playerColors.put("BLUE", new Color(0.2f, 0.2f, 0.8f, 1f));
        playerColors.put("YELLOW", new Color(0.8f, 0.8f, 0.2f, 1f));
    }

//    private Color getPlayerColor(String colorName) {
//        if (colorName == null) return Color.WHITE; // Fallback color
//
//        // Convert to uppercase for case-insensitive lookup
//        String upperColor = colorName.toUpperCase();
//        Color color = playerColors.get(upperColor);
//
//        if (color == null) {
//            Gdx.app.error(TAG, "No color found for: " + colorName + ". Using fallback color.");
//            return Color.WHITE; // Fallback color
//        }
//
//        return color;
//    }


    public int getPawnAtScreenCoords(int screenX, int screenY, Camera camera, String currentPlayerColor) {
        Ray ray = camera.getPickRay(screenX, screenY);
        float minDist = Float.MAX_VALUE;
        int selectedPawn = -1;

        Gdx.app.log(TAG, "Screen click at: " + screenX + ", " + screenY);
        Gdx.app.log(TAG, "Ray origin: " + ray.origin + ", direction: " + ray.direction);
        Gdx.app.log(TAG, "Checking pawns for color: " + currentPlayerColor);

        // Get indices for current player's pawns
        List<Integer> validPawnIndices = getPawnIndicesForColor(currentPlayerColor);
        Gdx.app.log(TAG, "Found " + validPawnIndices.size() + " pawns for color " + currentPlayerColor);

        float selectionRadius = 1.5f;

        for (Integer globalIndex : validPawnIndices) {
            Vector3 pawnPos = pawnPositions.get(globalIndex);
            if (pawnPos != null) {
                Gdx.app.log(TAG, "Testing pawn " + globalIndex + " at position: " + pawnPos);

                if (Intersector.intersectRaySphere(ray, pawnPos, selectionRadius, intersection)) {
                    float dist = intersection.dst2(camera.position);
                    Gdx.app.log(TAG, "Hit pawn " + globalIndex + " at distance: " + dist);

                    if (dist < minDist) {
                        minDist = dist;
                        // Convert global index back to local index (0-3)
                        selectedPawn = globalIndex % 4;
                    }
                }
            }
        }

        if (selectedPawn != -1) {
            highlightPawn(selectedPawn);
            Gdx.app.log(TAG, String.format("Selected pawn %d at distance %f", selectedPawn, minDist));
        }

        return selectedPawn;
    }

    private List<Integer> getPawnIndicesForColor(String color) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < pawnInstances.size; i++) {
            ModelInstance pawn = pawnInstances.get(i);
            if (pawn == null) {
                continue;
            }
            if (pawn.userData != null && pawn.userData.toString().equals(color)) {
                indices.add(i);
            }
        }
        return indices;
    }

    private void highlightPawn(int pawnIndex) { //todo should add return pawn color so that we can see if its being render correctly
        if (pawnIndex < 0 || pawnIndex >= pawnInstances.size) {
            Gdx.app.error(TAG, "Invalid pawn index: " + pawnIndex);
            return;
        }

        ModelInstance pawn = pawnInstances.get(pawnIndex);
        if (pawn == null || pawn.userData == null) {
            Gdx.app.error(TAG, "Pawn or pawn color data is null");
            return;
        }

        String colorName = (String) pawn.userData;
        final Color baseColor = playerColors.get(colorName.toUpperCase());
        if (baseColor == null) {
            Gdx.app.error(TAG, "Could not find color for: " + colorName);
            return;
        }

        // Store final values for use in Timer task
        final float ambientR = baseColor.r * 0.5f;
        final float ambientG = baseColor.g * 0.5f;
        final float ambientB = baseColor.b * 0.5f;

        // Create a brighter version of the base color for highlighting
        Color highlightColor = new Color(
            Math.min(baseColor.r * 1.5f, 1f),
            Math.min(baseColor.g * 1.5f, 1f),
            Math.min(baseColor.b * 1.5f, 1f),
            1f
        );

        try {
            Material highlightMaterial = new Material(
                ColorAttribute.createDiffuse(highlightColor),
                ColorAttribute.createSpecular(1, 1, 1, 1),
                ColorAttribute.createAmbient(highlightColor.r * 0.5f,
                    highlightColor.g * 0.5f,
                    highlightColor.b * 0.5f,
                    1f)
            );

            // Apply highlight material
            for (Material mat : pawn.materials) {
                mat.clear();
                mat.set(highlightMaterial);
            }

            // Schedule reset of material
            Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    try {
                        Material originalMaterial = new Material(
                            ColorAttribute.createDiffuse(new Color(baseColor)),
                            ColorAttribute.createSpecular(1, 1, 1, 1),
                            ColorAttribute.createAmbient(ambientR, ambientG, ambientB, 1f)
                        );

                        if (pawn.materials != null) {
                            for (Material mat : pawn.materials) {
                                mat.clear();
                                mat.set(originalMaterial);
                            }
                        }
                    } catch (Exception e) {
                        Gdx.app.error(TAG, "Error resetting pawn material: " + e.getMessage());
                    }
                }
            }, 0.2f);
        } catch (Exception e) {
            Gdx.app.error(TAG, "Error applying highlight material: " + e.getMessage());
        }
    }

    @SuppressWarnings("unused")  // These methods are used for debugging and future features
    private void validateGridCoordinate(float x, float y, String source) {
        if (x < 0 || x > 15 || y < 0 || y > 15) {
            Gdx.app.error(TAG, String.format("[VALIDATE] Invalid grid coordinate (%f,%f) from %s", x, y, source));
        }
    }

    @SuppressWarnings("unused")  // These methods are used for debugging and future features
    private void validatePosition(float x, float y, String source) {
        if (x < 0 || x > BOARD_SIZE || y < 0 || y > BOARD_SIZE) {
            Gdx.app.error(TAG, String.format("[VALIDATE] Invalid position (%f,%f) from %s", x, y, source));
        }
    }

    public void setCurrentPlayerColor(String color) {
        this.currentPlayerColor = color;
    }

    // For ray picking
    public boolean checkPawnHit(Ray ray, String playerColor, Vector3 outPosition) {
        float minDist = Float.MAX_VALUE;
        boolean hit = false;

        for (int i = 0; i < pawnInstances.size; i++) {
            ModelInstance pawn = pawnInstances.get(i);
            if (pawn != null && getPawnColor(i).equals(playerColor)) {
                Vector3 position = pawn.transform.getTranslation(new Vector3());
                float dist = ray.origin.dst(position);

                // Check if ray hits pawn's bounding sphere
                if (Intersector.intersectRaySphere(ray, position, PAWN_RADIUS, null)) {
                    if (dist < minDist) {
                        minDist = dist;
                        outPosition.set(position);
                        hit = true;
                    }
                }
            }
        }

        return hit;
    }

    // Helper method to get pawn color based on index
    private String getPawnColor(int index) {
        int colorIndex = index / 4;  // Each color has 4 pawns
        switch (colorIndex) {
            case 0: return "RED";
            case 1: return "BLUE";
            case 2: return "GREEN";
            case 3: return "YELLOW";
            default: return "RED";
        }
    }

//    private Vector3 calculateTargetPosition(String playerColor, int boardPosition) {
//        try {
//            // Handle home base (-1)
//            if (boardPosition == -1) {
//                int pawnIndex = pawnIndicesInBase.getOrDefault(playerColor, 0) % 4;
//                pawnIndicesInBase.put(playerColor, pawnIndicesInBase.getOrDefault(playerColor, 0) + 1);
//                return BoardCoordinates.getHomeBasePosition(playerColor, pawnIndex);
//            }
//
//            // Handle home column positions (>=52)
//            if (boardPosition >= gameBoard.getTotalSpaces()) {
//                int homeStep = boardPosition - gameBoard.getTotalSpaces();
//                return BoardCoordinates.getHomeColumnPosition(playerColor, homeStep);
//            }
//
//            // Handle main path positions
//            return BoardCoordinates.getMainPathPosition(boardPosition);
//        } catch (Exception e) {
//            Gdx.app.error(TAG, "Error calculating position: " + e.getMessage(), e);
//            return new Vector3(0, 0.2f, 0);
//        }
//    }

    public void updatePawnPosition(int pawnIndex, int newPosition, String playerColor) {
        Gdx.app.log(TAG, String.format("[POSITION] Updating %s pawn %d to position %d", playerColor, pawnIndex, newPosition));

        int globalPawnIndex = getGlobalPawnIndex(playerColor, pawnIndex);

        // Ensure pawn instance exists
        if (pawnInstances.get(globalPawnIndex) == null) {
            ModelInstance newPawn = createPawnInstance(playerColor);
            pawnInstances.set(globalPawnIndex, newPawn);
        }

        Vector3 targetPos = calculateTargetPosition(playerColor, newPosition);
        Vector3 currentPos = pawnPositions.getOrDefault(globalPawnIndex, targetPos.cpy());

        // Log the readable grid coordinates
        String readableCoords = BoardCoordinates.worldToGrid(targetPos);
        Gdx.app.log(TAG, String.format("[GRID] %s pawn %d moving to grid position %s", playerColor, pawnIndex, readableCoords));

        // Only create animation if the position has changed
        if (!currentPos.equals(targetPos)) {
            Gdx.app.log(TAG, String.format("[ANIMATION] %s pawn moving from %s to %s", playerColor, currentPos.toString(), targetPos.toString()));
            PawnAnimation animation = new PawnAnimation(currentPos, targetPos, playerColor);
            pawnAnimations.put(globalPawnIndex, animation);
            pawnPositions.put(globalPawnIndex, targetPos);
        }
    }

    private Vector3 calculateTargetPosition(String playerColor, int boardPosition) {
        try {
            // Handle home base (-1)
            if (boardPosition == -1) {
                int pawnIndex = pawnIndicesInBase.getOrDefault(playerColor, 0) % 4;
                pawnIndicesInBase.put(playerColor, pawnIndicesInBase.getOrDefault(playerColor, 0) + 1);
                return BoardCoordinates.getHomeBasePosition(playerColor, pawnIndex);
            }

            // Handle home column positions (>=52)
            if (boardPosition >= gameBoard.getTotalSpaces()) {
                int homeStep = boardPosition - gameBoard.getTotalSpaces();
                return BoardCoordinates.getHomeColumnPosition(playerColor, homeStep);
            }

            // Handle main path positions
            return BoardCoordinates.getMainPathPosition(boardPosition);
        } catch (Exception e) {
            Gdx.app.error(TAG, "Error calculating position: " + e.getMessage(), e);
            return new Vector3(0, 0.2f, 0);
        }
    }

    private int getGlobalPawnIndex(String playerColor, int localPawnIndex) {
        // Calculate offset based on player color
        int colorOffset = 0;
        switch(playerColor.toUpperCase()) {
            case "RED": colorOffset = 0; break;
            case "BLUE": colorOffset = 4; break;
            case "GREEN": colorOffset = 8; break;
            case "YELLOW": colorOffset = 12; break;
        }
        return colorOffset + localPawnIndex;
    }

//    public void updatePawnPosition(int pawnIndex, int newPosition, String playerColor) {
//        Gdx.app.log(TAG, String.format("[POSITION] Updating %s pawn %d to position %d", playerColor, pawnIndex, newPosition));
//
//        int colorOffset = getColorOffset(playerColor);
//        int globalPawnIndex = colorOffset + (pawnIndex % 4);
//
//        // Ensure pawn instance exists
//        while (pawnInstances.size <= globalPawnIndex) {
//            ModelInstance newPawn = createPawnInstance(playerColor);
//            pawnInstances.add(newPawn);
//        }
//
//        Vector3 targetPos = calculateTargetPosition(playerColor, newPosition);
//        Vector3 currentPos = pawnPositions.getOrDefault(globalPawnIndex, targetPos.cpy());
//
//        // Log the readable grid coordinates
//        String readableCoords = BoardCoordinates.worldToGrid(targetPos);
//        Gdx.app.log(TAG, String.format("[GRID] %s pawn %d moving to grid position %s", playerColor, pawnIndex, readableCoords));
//
//        // Only create animation if the position has changed
//        if (!currentPos.equals(targetPos)) {
//            Gdx.app.log(TAG, String.format("[ANIMATION] %s pawn moving from %s to %s", playerColor, currentPos.toString(), targetPos.toString()));
//            PawnAnimation animation = new PawnAnimation(currentPos, targetPos, playerColor);
//            pawnAnimations.put(globalPawnIndex, animation);
//            pawnPositions.put(globalPawnIndex, targetPos);
//        }
//    }
//
//    private Vector3 calculateTargetPosition(String playerColor, int boardPosition) {
//        try {
//            // Handle home base (-1)
//            if (boardPosition == -1) {
//                int pawnIndex = pawnIndicesInBase.getOrDefault(playerColor, 0) % 4;
//                pawnIndicesInBase.put(playerColor, pawnIndicesInBase.getOrDefault(playerColor, 0) + 1);
//                return BoardCoordinates.getHomeBasePosition(playerColor, pawnIndex);
//            }
//
//            // Handle home column positions (>=52)
//            if (boardPosition >= gameBoard.getTotalSpaces()) {
//                int homeStep = boardPosition - gameBoard.getTotalSpaces();
//                return BoardCoordinates.getHomeColumnPosition(playerColor, homeStep);
//            }
//
//            // Handle main path positions
//            return BoardCoordinates.getMainPathPosition(boardPosition);
//        } catch (Exception e) {
//            Gdx.app.error(TAG, "Error calculating position: " + e.getMessage(), e);
//            return new Vector3(0, 0.2f, 0);
//        }
//    }

    private static class PawnAnimation {
        private final Vector3 startPos;
        private final Vector3 targetPos;
        private final Vector3 controlPoint;
        private float progress;
        private static final float ANIMATION_SPEED = 2.0f;
        private static final float ARC_HEIGHT = 0.8f;

        PawnAnimation(Vector3 start, Vector3 target, String color) {
            this.startPos = start.cpy();
            this.targetPos = target.cpy();

            // Calculate a control point for arc movement
            this.controlPoint = new Vector3(
                (start.x + target.x) * 0.5f,
                start.y + ARC_HEIGHT,
                (start.z + target.z) * 0.5f
            );
            this.progress = 0;

            Gdx.app.log("PawnAnimation", String.format(
                "Creating animation for %s pawn: Start(%s) -> Target(%s)",
                color, start.toString(), target.toString()));
        }

        Vector3 getCurrentPosition() {
            float t = progress;
            Vector3 currentPos = new Vector3();

            // Quadratic Bezier curve for smooth arc movement
            float oneMinusT = 1 - t;
            currentPos.x = oneMinusT * oneMinusT * startPos.x + 2 * oneMinusT * t * controlPoint.x + t * t * targetPos.x;
            currentPos.y = oneMinusT * oneMinusT * startPos.y + 2 * oneMinusT * t * controlPoint.y + t * t * targetPos.y;
            currentPos.z = oneMinusT * oneMinusT * startPos.z + 2 * oneMinusT * t * controlPoint.z + t * t * targetPos.z;

            return currentPos;
        }

        boolean update(float deltaTime) {
            progress = Math.min(1.0f, progress + deltaTime * ANIMATION_SPEED);
            Gdx.app.log("PawnAnimation", String.format(
                "Updating animation progress: %f, Current position: %s",
                progress, getCurrentPosition().toString()));
            return progress >= 1.0f;
        }
    }
}
