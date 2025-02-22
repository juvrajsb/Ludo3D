package ludo.client.render;

import com.badlogic.gdx.Gdx;
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
import ludo.core.entities.Pawn;
import ludo.core.entities.Player;
import ludo.core.utils.BoardCoordinates;

import java.util.*;

public class GameRenderer {
    private static final String TAG = "GameRenderer";
    private final float BOARD_SIZE = 15f;
    private final float PAWN_SCALE = 7f;
//    private final float BOARD_SCALE = 0.5f;
    private final Vector3 intersection;
    private BoardCoordinates boardCoordinates;
    private final Board gameBoard;
    private final Array<ModelInstance> pawnInstances;
    private final Map<Integer, Vector3> pawnPositions;
    private final DiceRenderer diceRenderer;
    private final Map<Integer, PawnAnimation> pawnAnimations;
    private final Map<String, Integer> pawnIndicesInBase = new HashMap<>();
    private Model boardModel;
    private ModelInstance boardInstance;
    private Map<String, Color> playerColors;
    private Texture boardTexture;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private SpriteBatch spriteBatch;

    public GameRenderer(int width, int height) {
        pawnInstances = new Array<>();
        pawnPositions = new HashMap<>();
        pawnAnimations = new HashMap<>();
        gameBoard = new Board();
        boardCoordinates = new BoardCoordinates();
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

    public void render(ModelBatch modelBatch, Environment environment, float delta) {
        updateAnimations(delta);

        if (boardInstance != null) {
            modelBatch.render(boardInstance, environment);
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

            Color playerColor = getPlayerColor(player.getColor());

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
                it.remove();
            } else {
                int pawnIndex = entry.getKey();
                ModelInstance pawn = pawnInstances.get(pawnIndex);
                Vector3 pos = anim.getCurrentPosition();
                pawn.transform.setTranslation(pos);
            }
        }
    }

    private void createBoard() {
        try {
            boardTexture = new Texture(Gdx.files.internal("images/board.png"));
            boardTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

            ModelBuilder modelBuilder = new ModelBuilder();
            Material material = new Material(TextureAttribute.createDiffuse(boardTexture));

            boardModel = modelBuilder.createBox(
                BOARD_SIZE,
                0.1f, // height/thickness of board
                BOARD_SIZE,
                material,
                Usage.Position | Usage.Normal | Usage.TextureCoordinates
            );

            boardInstance = new ModelInstance(boardModel);
            boardInstance.transform.translate(0, 0, 0);

        } catch (Exception e) {
            Gdx.app.error(TAG, "Error creating board: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private ModelInstance createPawnInstance(String color) {
        Model pawnModel = GameAssets.getInstance().getPawnModel();
        if (pawnModel == null) {
            Gdx.app.error(TAG, "Pawn model is null!");
            return null;
        }

        ModelInstance pawn = new ModelInstance(pawnModel);

        Color pawnColor = getPlayerColor(color);
        Material pawnMaterial = new Material(
            ColorAttribute.createDiffuse(pawnColor),
            ColorAttribute.createSpecular(1, 1, 1, 1),
            ColorAttribute.createAmbient(pawnColor.r * 0.5f,
                pawnColor.g * 0.5f,
                pawnColor.b * 0.5f,
                1f)
        );

        for (Material mat : pawn.materials) {
            mat.clear();
            mat.set(pawnMaterial);
        }

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

//    private Vector3 calculateHomeBasePosition(String color, int index) {
//        switch(color.toUpperCase()) {
//            case "RED": // Bottom right
//                return boardCoordinates.gridToWorld(13 + (index % 2), 13 + (index / 2));
//            case "BLUE": // Bottom left
//                return gridToWorld(1 + (index % 2), 13 + (index / 2));
//            case "GREEN": // Top left
//                return gridToWorld(1 + (index % 2), 1 + (index / 2));
//            case "YELLOW": // Top right
//                return gridToWorld(13 + (index % 2), 1 + (index / 2));
//            default:
//                return new Vector3(0, 0, 0);
//        }
//    }

//    private Vector3 gridToWorld(int x, int y) {
//        // Convert grid coordinates (0-14) to world space
//        float worldX = (x - BOARD_SIZE/2);// * BOARD_SCALE;
//        float worldZ = (BOARD_SIZE/2 - y);// * BOARD_SCALE;
//        Vector3 result = new Vector3(worldX, 0, worldZ);
////        Gdx.app.log(TAG, String.format(
////            "Grid->World conversion: (%d,%d) -> (%f,%f,%f), BOARD_SIZE=%f, BOARD_SCALE=%f",
////            x, y, result.x, result.y, result.z, BOARD_SIZE, BOARD_SCALE
////        ));
//        return result;
//    }

    private void positionPawn(ModelInstance pawnInstance, int boardPosition, int pawnIndex, String playerColor) {
        Vector3 position;

        if (boardPosition == -1) {
            int baseX = getHomeBaseStartX(playerColor);
            int baseY = getHomeBaseStartY(playerColor);

            int x = baseX + (pawnIndex % 2);
            int y = baseY + (pawnIndex / 2);
            Gdx.app.log(TAG, String.format("Positioning %s pawn %d in home base at (%d,%d) from base (%d,%d)",
                playerColor, pawnIndex, x, y, baseX, baseY));

            position = boardCoordinates.gridToWorld(x, y);
        } else {
            int[] gridCoords = BoardCoordinates.getGridCoordsForPosition(boardPosition);
            if (gridCoords == null || gridCoords.length < 2) {
                Gdx.app.error(TAG, "Invalid board position: " + boardPosition);
                return;
            }
            position = boardCoordinates.gridToWorld(gridCoords[0], gridCoords[1]);
        }

        Gdx.app.log(TAG, String.format("Positioning %s pawn %d at board position %d -> world position %s",
            playerColor, pawnIndex, boardPosition, position.toString()));

        pawnInstance.transform.setToTranslation(position);
        pawnInstance.transform.scale(PAWN_SCALE, PAWN_SCALE, PAWN_SCALE);
        pawnInstance.userData = playerColor;
        pawnPositions.put(pawnIndex, position);
    }

    private int getHomeBaseStartX(String color) {
        switch(color.toUpperCase()) {
            case "BLUE":
            case "YELLOW":
                return 0;  // Right side of board
            case "RED":
            case "GREEN":
                return 13;   // Left side of board
            default:
                return 0;
        }
    }

    private int getHomeBaseStartY(String color) {
        switch(color.toUpperCase()) {
            case "RED":
            case "BLUE":
                return 13;  // Bottom of board
            case "YELLOW":
            case "GREEN":
                return 0;   // Top of board
            default:
                return 0;
        }
    }

//    public void clearHighlights() {
//        for (Map.Entry<Integer, Boolean> entry : highlightedPawns.entrySet()) {
//            int pawnIdx = entry.getKey();
//            if (pawnIdx < pawnInstances.size) {
//                ModelInstance pawnInstance = pawnInstances.get(pawnIdx);
//                if (pawnInstance != null && pawnInstance.userData != null) {
//                    Material mat = pawnInstance.materials.get(0);
//                    mat.set(ColorAttribute.createDiffuse(
//                        playerColors.get(pawnInstance.userData.toString())));
//                }
//            }
//        }
//        highlightedPawns.clear();
//    }

//    public boolean isPawnHighlighted(int pawnIndex) {//TODO check usage not used currently
//        return highlightedPawns.containsKey(pawnIndex);
//    }

//    public void updateAllPawnPositions() {
//        for (int i = 0; i < pawnInstances.size; i++) {
//            ModelInstance pawn = pawnInstances.get(i);
//            Vector3 position = pawnPositions.get(i);
//
//            // If position is null, calculate it based on the pawn's initial state
//            if (position == null) {
//                String playerColor = (String) pawn.userData;
//                int localPawnIndex = i % 4;
//                position = calculateHomeBasePosition(playerColor, localPawnIndex);
//                pawnPositions.put(i, position);
//            }
//
//            Gdx.app.log(TAG, "METHOD:[UPDATEALLPAWNPOSITIONS] Updating pawn " + i + " position to " + position);
//            if (position != null) {
//                pawn.transform.setToTranslation(position);
//                pawn.transform.scale(PAWN_SCALE, PAWN_SCALE, PAWN_SCALE);
//            }
//        }
//    }

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

    public int getPawnAtScreenCoords(int screenX, int screenY, Camera camera, String currentPlayerColor) {
        Ray ray = camera.getPickRay(screenX, screenY);
        float minDist = Float.MAX_VALUE;
        int selectedPawn = -1;

//        Gdx.app.log(TAG, "Screen click at: " + screenX + ", " + screenY);
//        Gdx.app.log(TAG, "Ray origin: " + ray.origin + ", direction: " + ray.direction);
        Gdx.app.log(TAG, "Checking pawns for color: " + currentPlayerColor);

        // Get indices for current player's pawns
        List<Integer> validPawnIndices = getPawnIndicesForColor(currentPlayerColor);
        Gdx.app.log(TAG, "Found " + validPawnIndices.size() + " pawns for color " + currentPlayerColor);

        float selectionRadius = 1.5f;

        for (Integer globalIndex : validPawnIndices) {
            Vector3 pawnPos = pawnPositions.get(globalIndex);
            if (pawnPos != null) {
//                Gdx.app.log(TAG, "Testing pawn " + globalIndex + " at position: " + pawnPos);

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

//    public void setCurrentPlayerColor(String color) {
//        this.currentPlayerColor = color;
//    }

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

    public void updatePawnPosition(int pawnIndex, int newPosition, String playerColor) {
//        Gdx.app.log(TAG, String.format("[POSITION] Updating %s pawn %d to position %d", playerColor, pawnIndex, newPosition));

        int globalPawnIndex = getGlobalPawnIndex(playerColor, pawnIndex);

//        Gdx.app.log(TAG, String.format("[INIT] Pawn global index: %d, Player color: %s, Local index: %d",
//            globalPawnIndex, playerColor, pawnIndex));

        // Current initial position calculation
        Vector3 initialPos = pawnPositions.get(globalPawnIndex);
//        Gdx.app.log(TAG, String.format("[HOME] Initial home base for %s pawn %d should be in range %s, current position: %s",
//            playerColor, pawnIndex,
//            playerColor.equals("RED") ? "9:15,9:15" :
//                playerColor.equals("GREEN") ? "9:15,0:6" :
//                    playerColor.equals("BLUE") ? "0:6,9:15" : "0:6,0:6",
//            initialPos != null ? initialPos.toString() : "null"));

        // Add target position calculation logging
        if (newPosition == -1) {
            // This is a home base position
            int baseX = playerColor.equals("RED") || playerColor.equals("YELLOW") ? 13 : 1;
            int baseY = playerColor.equals("RED") || playerColor.equals("BLUE") ? 13 : 1;
//            Gdx.app.log(TAG, String.format("[CALC] Home base starting coordinates for %s: (%d,%d)",
//                playerColor, baseX, baseY));
        }

        if (pawnInstances.get(globalPawnIndex) == null) {
            ModelInstance newPawn = createPawnInstance(playerColor);
            pawnInstances.set(globalPawnIndex, newPawn);
        }

        Vector3 targetPos = calculateTargetPosition(playerColor, newPosition);
        Vector3 currentPos = pawnPositions.getOrDefault(globalPawnIndex, targetPos.cpy());

        String readableCoords = BoardCoordinates.worldToGrid(targetPos);
        String readableCurrent = BoardCoordinates.worldToGrid(currentPos);
//        Gdx.app.log(TAG, String.format("[GRID] %s pawn %d moving to grid position %s from %s", playerColor, pawnIndex, readableCoords, readableCurrent));

        if (!currentPos.equals(targetPos)) {
            PawnAnimation animation = new PawnAnimation(currentPos, targetPos, playerColor);//todo highlight
            pawnAnimations.put(globalPawnIndex, animation);
            pawnPositions.put(globalPawnIndex, targetPos);
        }
    }

    private Vector3 calculateTargetPosition(String playerColor, int boardPosition) { //todo should use pawn or board class
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
            return new Vector3(0, 0, 0);
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
}
