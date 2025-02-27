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
import ludo.core.utils.Constants;

import java.util.*;

public class GameRenderer {
    private static final String TAG = "GameRenderer";
    private final float BOARD_SIZE = 15f;
    private static final Vector3 POSITION_OFFSET = new Vector3(0.5f, 0, 0.5f);
    private final float PAWN_SCALE = 7f;
    private final Vector3 intersection;
    private BoardCoordinates boardCoordinates;
    private final Array<ModelInstance> pawnInstances;
    private final Map<Integer, Vector3> pawnPositions;
    private final DiceRenderer diceRenderer;
    private final Map<String, Integer> pawnIndicesInBase = new HashMap<>();
    private Model boardModel;
    private ModelInstance boardInstance;
    private Map<String, Color> playerColors;
    private Texture boardTexture;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private SpriteBatch spriteBatch;

    private Map<Integer, List<Integer>> stackedPositions = new HashMap<>();
    private Map<Integer, Float> pawnHeights = new HashMap<>();

    private final Map<Integer, PawnAnimation> pawnAnimations = new HashMap<>();
    private final Map<Integer, Boolean> movingPawns = new HashMap<>();
    private final Map<Integer, Boolean> selectablePawns = new HashMap<>();
    private boolean isAnyAnimationPlaying = false;
    private final List<Vector3> validMoveHighlights = new ArrayList<>();

    private static final float VERTICAL_STACK_OFFSET = 0.4f;
    private static final float SIDE_OFFSET_X = 0.4f;
    private static final float SIDE_OFFSET_Z = 0.2f;


    public GameRenderer(int width, int height) {
        pawnInstances = new Array<>();
        pawnPositions = new HashMap<>();
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
        isAnyAnimationPlaying = !pawnAnimations.isEmpty(); //todo comment after

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

        renderHighlights(modelBatch, environment);
    }

    private void renderHighlights(ModelBatch modelBatch, Environment environment) {
        // This is a placeholder
    }

    public void startDiceRollAnimation() {
        // The actual value will be updated when server responds
        int initialValue = 1 + (int)(Math.random() * 6);
        diceRenderer.startRoll(initialValue);
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
                    1f)
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
                pawnInstance.userData = player.getColor();
                Gdx.app.log(TAG, "Created pawn " + globalPawnIndex + " at position " + position +
                    " for player " + player.getColor());
            }
        }

        Gdx.app.log(TAG, String.format("Finished creating pawns. Total pawnInstance: %d", pawnInstances.size));
    }

    protected static Vector3 applyOffset(Vector3 position) {
        return position.cpy().add(POSITION_OFFSET);
    }

    public boolean isAnimating() {
        return isAnyAnimationPlaying || !pawnAnimations.isEmpty();
    }


    public void updateAnimations(float delta) {
        Iterator<Map.Entry<Integer, PawnAnimation>> it = pawnAnimations.entrySet().iterator();
        boolean animationsActive = false;

        while (it.hasNext()) {
            Map.Entry<Integer, PawnAnimation> entry = it.next();
            PawnAnimation anim = entry.getValue();
            if (anim.update(delta)) {
                it.remove();
            } else {
                animationsActive = true;
                int pawnIndex = entry.getKey();
                ModelInstance pawn = pawnInstances.get(pawnIndex);
                Vector3 pos = anim.getCurrentPosition();
                pawn.transform.setTranslation(pos);
            }
        }

        isAnyAnimationPlaying = animationsActive;
    }

    public void setPawnMovingState(int pawnIndex, boolean isMoving) {
        movingPawns.put(pawnIndex, isMoving);

        ModelInstance pawn = pawnInstances.get(pawnIndex);
        if (pawn != null) {
            Material material = pawn.materials.get(0);
            String playerColor = (String) pawn.userData;

            if (isMoving) {
                Color baseColor = playerColors.get(playerColor);
                if (baseColor != null) {
                    Color highlightColor = new Color(
                        Math.min(baseColor.r * 1.3f, 1f),
                        Math.min(baseColor.g * 1.3f, 1f),
                        Math.min(baseColor.b * 1.3f, 1f),
                        1f
                    );

                    material.set(ColorAttribute.createDiffuse(highlightColor));
                }
            } else {
                Color baseColor = playerColors.get(playerColor);
                if (baseColor != null) {
                    material.set(ColorAttribute.createDiffuse(baseColor));
                }
            }
        }
    }

    public void setSelectablePawns(List<Integer> selectablePawnIndices) {
        selectablePawns.clear();

        for (Integer index : selectablePawnIndices) {
            selectablePawns.put(index, true);
        }

        updateSelectablePawnsVisuals();
    }


    private void updateSelectablePawnsVisuals() {
        validMoveHighlights.clear();

        for (Map.Entry<Integer, Boolean> entry : selectablePawns.entrySet()) {
            if (entry.getValue()) {
                int pawnIndex = entry.getKey();
                ModelInstance pawn = pawnInstances.get(pawnIndex);

                if (pawn != null) {

                    Vector3 position = pawnPositions.get(pawnIndex);
                    if (position != null) {
                        validMoveHighlights.add(position.cpy().add(0, 0.1f, 0));
                    }
                }
            }
        }
    }

    public void flashInvalidSelection(int pawnIndex) {
        ModelInstance pawn = pawnInstances.get(pawnIndex);
        if (pawn == null) return;

        String playerColor = (String) pawn.userData;
        final Color originalColor = playerColors.get(playerColor);

        if (originalColor == null) return;

        Material material = pawn.materials.get(0);
        material.set(ColorAttribute.createDiffuse(Color.RED));

        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                material.set(ColorAttribute.createDiffuse(originalColor));
            }
        }, 0.3f);
    }

    public void highlightValidMoves(List<Integer> validPositions) {
        validMoveHighlights.clear();

        for (Integer position : validPositions) {
            Vector3 worldPos;

            if (position < 0) {
                continue;
            } else if (position >= Constants.BOARD_SIZE) {
                // Home column position
            } else {
                // Main board position
                worldPos = BoardCoordinates.getMainPathPosition(position);
                validMoveHighlights.add(worldPos.cpy().add(0, 0.1f, 0));
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

    private void positionPawn(ModelInstance pawnInstance, int boardPosition, int pawnIndex, String playerColor) {
        Vector3 position;

        Gdx.app.log("PawnPositioning", String.format(
            "Processing pawn - Color: %s, Global Index: %d, Base Coords: (%d,%d)",
            playerColor, pawnIndex,
            getHomeBaseStartX(playerColor), getHomeBaseStartY(playerColor)
        ));

        if (boardPosition == -1) {
            position = boardCoordinates.getHomeBasePosition(playerColor, pawnIndex % 4);
        } else {
            int[] gridCoords = BoardCoordinates.getGridCoordsForPosition(boardPosition);
            if (gridCoords == null || gridCoords.length < 2) {
                Gdx.app.error(TAG, "Invalid board position: " + boardPosition);
                return;
            }
            position = boardCoordinates.gridToWorld(gridCoords[0], gridCoords[1]);
            position = applyOffset(position);
        }
        pawnInstance.transform.setToTranslation(position);
        pawnInstance.transform.scale(PAWN_SCALE, PAWN_SCALE, PAWN_SCALE);
        pawnInstance.userData = playerColor;
        pawnPositions.put(pawnIndex, position);
    }

    private int getHomeBaseStartX(String color) {
        switch(color.toUpperCase()) {
            case "RED":
            case "GREEN":
                return 9;  // Right side starts at 9
            case "BLUE":
            case "YELLOW":
                return 0;  // Left side starts at 0
            default:
                return 0;
        }
    }

    private int getHomeBaseStartY(String color) {
        switch(color.toUpperCase()) {
            case "RED":
            case "BLUE":
                return 9;  // Top of board starts at 9
            case "YELLOW":
            case "GREEN":
                return 0;  // Bottom of board starts at 0
            default:
                return 0;
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

    public int getPawnAtScreenCoords(int screenX, int screenY, Camera camera, String currentPlayerColor) {
        Ray ray = camera.getPickRay(screenX, screenY);
        float minDist = Float.MAX_VALUE;
        int selectedPawn = -1;

        Gdx.app.log(TAG, "Screen click at: " + screenX + ", " + screenY);
        Gdx.app.log(TAG, "Checking pawns for color: " + currentPlayerColor);

        // Get indices for current player's pawns
        List<Integer> validPawnIndices = getPawnIndicesForColor(currentPlayerColor);
        Gdx.app.log(TAG, "Found " + validPawnIndices.size() + " pawns for color " + currentPlayerColor);

        float selectionRadius = 1.5f;

        List<Integer> heightSortedPawns = new ArrayList<>(validPawnIndices);
        heightSortedPawns.sort((a, b) -> {
            float heightA = pawnHeights.getOrDefault(a, 0f);
            float heightB = pawnHeights.getOrDefault(b, 0f);
            return Float.compare(heightB, heightA); // Descending order (highest first)
        });


        for (Integer globalIndex : validPawnIndices) {
            Vector3 pawnPos = pawnPositions.get(globalIndex);
            if (pawnPos != null) {
                Vector3 collisionPos = pawnPos.cpy().sub(POSITION_OFFSET);

                if (Intersector.intersectRaySphere(ray, collisionPos, selectionRadius, intersection)) {
                    float dist = intersection.dst2(camera.position);
                    Gdx.app.log(TAG, "Hit pawn " + globalIndex + " at distance: " + dist);

                    if (dist < minDist) {
                        minDist = dist;
                        selectedPawn = globalIndex % 4;
                    }
                }
            }
        }

        if (selectedPawn != -1) {
//            highlightPawn(selectedPawn);
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

//    private void highlightPawn(int pawnIndex) { //todo not used also should add return pawn color so that we can see if its being render correctly
//        if (pawnIndex < 0 || pawnIndex >= pawnInstances.size) {
//            Gdx.app.error(TAG, "Invalid pawn index: " + pawnIndex);
//            return;
//        }
//
//        ModelInstance pawn = pawnInstances.get(pawnIndex);
//        if (pawn == null || pawn.userData == null) {
//            Gdx.app.error(TAG, "Pawn or pawn color data is null");
//            return;
//        }
//
//        String colorName = (String) pawn.userData;
//        final Color baseColor = playerColors.get(colorName.toUpperCase());
//        if (baseColor == null) {
//            Gdx.app.error(TAG, "Could not find color for: " + colorName);
//            return;
//        }
//
//        // Store final values for use in Timer task
//        final float ambientR = baseColor.r * 0.5f;
//        final float ambientG = baseColor.g * 0.5f;
//        final float ambientB = baseColor.b * 0.5f;
//
//        // Create a brighter version of the base color for highlighting
//        Color highlightColor = new Color(
//            Math.min(baseColor.r * 1.5f, 1f),
//            Math.min(baseColor.g * 1.5f, 1f),
//            Math.min(baseColor.b * 1.5f, 1f),
//            1f
//        );
//
//        try {
//            Material highlightMaterial = new Material(
//                ColorAttribute.createDiffuse(highlightColor),
//                ColorAttribute.createSpecular(1, 1, 1, 1),
//                ColorAttribute.createAmbient(highlightColor.r * 0.5f,
//                    highlightColor.g * 0.5f,
//                    highlightColor.b * 0.5f,
//                    1f)
//            );
//
//            // Apply highlight material
//            for (Material mat : pawn.materials) {
//                mat.clear();
//                mat.set(highlightMaterial);
//            }
//
//            // Schedule reset of material
//            Timer.schedule(new Timer.Task() {
//                @Override
//                public void run() {
//                    try {
//                        Material originalMaterial = new Material(
//                            ColorAttribute.createDiffuse(new Color(baseColor)),
//                            ColorAttribute.createSpecular(1, 1, 1, 1),
//                            ColorAttribute.createAmbient(ambientR, ambientG, ambientB, 1f)
//                        );
//
//                        if (pawn.materials != null) {
//                            for (Material mat : pawn.materials) {
//                                mat.clear();
//                                mat.set(originalMaterial);
//                            }
//                        }
//                    } catch (Exception e) {
//                        Gdx.app.error(TAG, "Error resetting pawn material: " + e.getMessage());
//                    }
//                }
//            }, 0.2f);
//        } catch (Exception e) {
//            Gdx.app.error(TAG, "Error applying highlight material: " + e.getMessage());
//        }
//    }

    public void updatePawnPosition(int pawnIndex, int newPosition, String playerColor) {
        int globalPawnIndex = getGlobalPawnIndex(playerColor, pawnIndex);

        Vector3 initialPos = pawnPositions.get(globalPawnIndex);
        Vector3 targetPos = calculateTargetPosition(playerColor, newPosition);

        targetPos = applyStackingOffset(globalPawnIndex, targetPos, newPosition, playerColor);

        if (initialPos == null || !initialPos.equals(targetPos)) {
            PawnAnimation animation = new PawnAnimation(
                initialPos != null ? initialPos : targetPos.cpy(),
                targetPos,
                playerColor
            );
            pawnAnimations.put(globalPawnIndex, animation);
            pawnPositions.put(globalPawnIndex, targetPos);
        }
    }

    private Vector3 applyStackingOffset(int pawnIndex, Vector3 basePosition, int boardPosition, String pawnColor) {
        if (!stackedPositions.containsKey(boardPosition)) {
            stackedPositions.put(boardPosition, new ArrayList<>());
        }

        List<Integer> pawnsAtPosition = stackedPositions.get(boardPosition);

        for (List<Integer> stack : stackedPositions.values()) {
            stack.remove(Integer.valueOf(pawnIndex));
        }

        if (!pawnsAtPosition.contains(pawnIndex)) {
            pawnsAtPosition.add(pawnIndex);
        }

        Vector3 offsetPosition = basePosition.cpy();

        boolean useVerticalStacking = true;

        if (useVerticalStacking) {
            int stackIndex = pawnsAtPosition.indexOf(pawnIndex);
            float heightOffset = stackIndex * VERTICAL_STACK_OFFSET;
            pawnHeights.put(pawnIndex, heightOffset);
            offsetPosition.y = heightOffset;
        } else {
            int stackIndex = pawnsAtPosition.indexOf(pawnIndex);
            if (stackIndex > 0) {
                float angle = (360f / pawnsAtPosition.size()) * stackIndex;
                float radian = (float) Math.toRadians(angle);

                offsetPosition.x += SIDE_OFFSET_X * Math.cos(radian);
                offsetPosition.z += SIDE_OFFSET_Z * Math.sin(radian);
            }
        }

        return offsetPosition;
    }

    private Vector3 calculateTargetPosition(String playerColor, int boardPosition) {
        try {
            if (boardPosition == -1) {
                int pawnIndex = pawnIndicesInBase.getOrDefault(playerColor, 0) % 4;
                pawnIndicesInBase.put(playerColor, pawnIndicesInBase.getOrDefault(playerColor, 0) + 1);
                return BoardCoordinates.getHomeBasePosition(playerColor, pawnIndex);
            }

            if (boardPosition >= Constants.BOARD_SIZE) {
                int homeColumnStart = Constants.BOARD_SIZE;
                int colorOffset;

                switch(playerColor.toUpperCase()) {
                    case "YELLOW": colorOffset = 0; break;
                    case "BLUE": colorOffset = 1; break;
                    case "RED": colorOffset = 2; break;
                    case "GREEN": colorOffset = 3; break;
                    default: throw new IllegalArgumentException("Invalid color: " + playerColor);
                }
                int homeStep = boardPosition - (homeColumnStart + colorOffset * Constants.HOME_COLUMN_SIZE);
                if (homeStep >= 0 && homeStep < Constants.HOME_COLUMN_SIZE) {
                    return BoardCoordinates.getHomeColumnPosition(playerColor, homeStep);
                }
            }
            return BoardCoordinates.getMainPathPosition(boardPosition);

        } catch (Exception e) {
            Gdx.app.error(TAG, "Error calculating position: " + e.getMessage(), e);
            return new Vector3(0, 0, 0);
        }
    }

    private int getGlobalPawnIndex(String playerColor, int localPawnIndex) {
        int colorOffset = 0;
        switch(playerColor.toUpperCase()) {
            case "YELLOW": colorOffset = 0; break;
            case "BLUE": colorOffset = 4; break;
            case "RED": colorOffset = 8; break;
            case "GREEN": colorOffset = 12; break;
        }
        int globalIndex = colorOffset + localPawnIndex;

        Gdx.app.log("PawnIndexing", String.format(
            "Color: %s, Local Index: %d, Color Offset: %d, Global Index: %d",
            playerColor, localPawnIndex, colorOffset, globalIndex
        ));

        return globalIndex;
    }
}
