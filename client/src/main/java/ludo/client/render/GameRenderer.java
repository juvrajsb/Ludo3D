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
import ludo.core.entities.Board;
import ludo.core.entities.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import ludo.core.entities.Pawn;
public class GameRenderer {
    private static final String TAG = "GameRenderer";
    private static final Color HIGHLIGHT_COLOR = new Color(1, 1, 0, 0.5f);
    private static final float ANIMATION_DURATION = 0.5f;
    private final Vector3 tempVector = new Vector3();
    private final float BOARD_SIZE = 8f;
    private final float PAWN_SCALE = 2f;
    private final Ray ray;
    private final Vector3 intersection;
    private final float CELL_SIZE = BOARD_SIZE / 15f; // 15x15 grid
    private final Board gameBoard;
    private Model boardModel;
    private ModelInstance boardInstance;
    private Array<ModelInstance> pawnInstances;
    private Map<Integer, Vector3> pawnPositions;
    private Map<Integer, Boolean> highlightedPawns = new HashMap<>();
    private Map<String, Color> playerColors;
    private Texture boardTexture;
    private DiceRenderer diceRenderer;
    private Map<Integer, PawnAnimation> pawnAnimations;

    public GameRenderer(int width, int height) {
        Gdx.app.log(TAG, "Initializing GameRenderer");
        pawnInstances = new Array<>();
        pawnPositions = new HashMap<>();
        pawnAnimations = new HashMap<>();
        ray = new Ray();
        gameBoard = new Board();
        intersection = new Vector3();
        diceRenderer = new DiceRenderer();
        initializePlayerColors();
        createBoard();
    }

    public void debugRayTest(int screenX, int screenY, Camera camera) {
        ray.set(camera.getPickRay(screenX, screenY));
        Gdx.app.log(TAG, "Testing ray intersection at screen coords: " + screenX + ", " + screenY);

        for (int i = 0; i < pawnInstances.size; i++) {
            Vector3 pawnPos = pawnPositions.get(i);
            if (pawnPos != null) {
                float radius = CELL_SIZE * 0.4f;
                boolean hit = Intersector.intersectRaySphere(ray, pawnPos, radius, intersection);
                if (hit) {
                    Gdx.app.log(TAG, "Ray hit pawn " + i + " at position " + pawnPos);
                    Gdx.app.log(TAG, "Intersection point: " + intersection);
                }
            }
        }
    }

    public int getNumberOfPawns() {
        return pawnInstances.size;
    }

    public void updateDiceValue(int value) {
        diceRenderer.startRoll(value);
    }

    public void render(ModelBatch modelBatch, Environment environment, float delta) {
        updateAnimations(delta);

        // Render board and pawns
        if (boardInstance != null) {
            modelBatch.render(boardInstance, environment);
        }

        for (ModelInstance pawn : pawnInstances) {
            modelBatch.render(pawn, environment);
        }

        // Update and render dice
        diceRenderer.update(delta);
        diceRenderer.render(modelBatch);
    }

    private void updateAnimations(float delta) {
        Iterator<Map.Entry<Integer, PawnAnimation>> it = pawnAnimations.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, PawnAnimation> entry = it.next();
            PawnAnimation anim = entry.getValue();
            int pawnIndex = entry.getKey();

            anim.progress += delta / ANIMATION_DURATION;
            if (anim.progress >= 1.0f) {
                // Animation complete
                updatePawnTransform(pawnIndex, anim.targetPos);
                pawnPositions.put(pawnIndex, anim.targetPos);
                it.remove();
            } else {
                // Interpolate position
                Vector3 currentPos = new Vector3();
                currentPos.lerp(anim.targetPos, anim.progress);
                updatePawnTransform(pawnIndex, currentPos);
            }
        }
    }

    private void updatePawnTransform(int pawnIndex, Vector3 position) {
        if (pawnIndex < pawnInstances.size) {
            ModelInstance pawn = pawnInstances.get(pawnIndex);
            pawn.transform.setToTranslation(position);
            pawn.transform.scale(PAWN_SCALE, PAWN_SCALE, PAWN_SCALE);
        }
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
        // Get grid position from board
        Board.GridPosition gridPos = gameBoard.getGridPosition(boardPosition);

        // If position is -1 (home) or invalid, use home area position
        if (gridPos == null) {
            return calculateHomeAreaPosition(playerColor);
        }

        // Convert grid position to world coordinates
        float x = (gridPos.x * CELL_SIZE) - (BOARD_SIZE / 2) + (CELL_SIZE / 2);
        float z = (gridPos.y * CELL_SIZE) - (BOARD_SIZE / 2) + (CELL_SIZE / 2);
        float y = 0.2f; // Height above board

        return new Vector3(x, y, z);
    }

    private Vector3 calculateHomeAreaPosition(String playerColor) {
        float x = 0, z = 0;
        float offset = BOARD_SIZE * 0.4f; // Distance from center for home areas

        switch (playerColor.toUpperCase()) {
            case "RED":
                x = offset;
                z = offset;
                break;
            case "GREEN":
                x = -offset;
                z = offset;
                break;
            case "BLUE":
                x = offset;
                z = -offset;
                break;
            case "YELLOW":
                x = -offset;
                z = -offset;
                break;
        }

        return new Vector3(x, 0.2f, z);
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

    public void updatePawnPosition(int pawnIndex, int newPosition, String playerColor) {
        if (pawnIndex >= 0 && pawnIndex < pawnInstances.size) {
            Vector3 currentPos = pawnPositions.get(pawnIndex);
            Vector3 targetPos = calculatePawnPosition(newPosition, playerColor);

            // Start animation
            pawnAnimations.put(pawnIndex, new PawnAnimation(currentPos, targetPos));
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

        // Debug logging
        Gdx.app.log(TAG, "Screen click at: " + screenX + ", " + screenY);
        Gdx.app.log(TAG, "Ray origin: " + ray.origin + ", direction: " + ray.direction);

        // Larger selection radius for easier clicking
        float selectionRadius = 1.0f;  // Increased radius further

        for (int i = 0; i < pawnInstances.size; i++) {
            Vector3 pawnPos = pawnPositions.get(i);
            if (pawnPos != null) {
                // Debug each pawn position
                Gdx.app.log(TAG, "Testing pawn " + i + " at position: " + pawnPos);

                // Create a sphere around the pawn for selection
                if (Intersector.intersectRaySphere(ray, pawnPos, selectionRadius, intersection)) {
                    float dist = intersection.dst2(camera.position);
                    Gdx.app.log(TAG, "Hit pawn " + i + " at distance: " + dist);

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
            Gdx.app.log(TAG, "Selected pawn: " + selectedPawn + " at distance: " + minDist);
        } else {
            Gdx.app.log(TAG, "No pawn selected");
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

    private static class PawnAnimation {
        Vector3 startPos;
        Vector3 targetPos;
        float progress;

        PawnAnimation(Vector3 start, Vector3 target) {
            this.startPos = start.cpy();
            this.targetPos = target;
            this.progress = 0;
        }
    }
}
