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
    private Map<String, Color> playerColors;
    private final float BOARD_SIZE = 8f;
    private final float PAWN_SCALE = 0.15f;
    private final Ray ray;
    private final Vector3 intersection;
    private Texture boardTexture;
    private final float SQUARE_SIZE = BOARD_SIZE / 15f; // 15x15 grid

    public GameRenderer(int width, int height) {
        Gdx.app.log(TAG, "Initializing GameRenderer");
        pawnInstances = new Array<>();
        pawnPositions = new HashMap<>();
        ray = new Ray();
        intersection = new Vector3();
        initializePlayerColors();
        createBoard();
    }

    public int getNumberOfPawns() {
        return pawnInstances.size;
    }

    public void render(ModelBatch modelBatch, Environment environment) {
        // Render board
        if (boardInstance != null) {
            modelBatch.render(boardInstance, environment);
        }

        // Render pawns with position logging
        if (pawnInstances.size == 0) {
            Gdx.app.debug(TAG, "No pawns to render");
        } else {
            for (ModelInstance pawn : pawnInstances) {
                modelBatch.render(pawn, environment);
            }
            Gdx.app.debug(TAG, "Rendered " + pawnInstances.size + " pawns");
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

            for (Pawn pawn : player.getPawns()) {
                ModelInstance pawnInstance = new ModelInstance(pawnModel);
                pawnInstance.materials.get(0).set(pawnMaterial);

                int position = pawn.getPosition();
                positionPawn(pawnInstance, position, pawnIndex);

                pawnInstances.add(pawnInstance);
                Gdx.app.log(TAG, "Created pawn " + pawnIndex + " at position " + position);
                pawnIndex++;
            }
        }
        Gdx.app.log(TAG, "Finished creating pawns. Total: " + pawnInstances.size);
    }

    private Vector3 calculatePawnPosition(int boardPosition) {
        if (boardPosition == -1) { // Pawn in home
            // Return base/starting positions based on player color
            return new Vector3(-BOARD_SIZE/2 + 0.5f, 0.2f, -BOARD_SIZE/2 + 0.5f);
        }

        // Convert board position to x,z coordinates on grid
        // Board is centered at origin (0,0,0)
        float squareSize = BOARD_SIZE / 15f; // 15x15 grid
        float halfBoard = BOARD_SIZE / 2;

        // Calculate x and z positions
        float x = ((boardPosition % 15) * squareSize) - halfBoard;
        float z = ((boardPosition / 15) * squareSize) - halfBoard;

        // Y position is slightly above board
        float y = 0.2f;

        return new Vector3(x, y, z);
    }

    private void positionPawn(ModelInstance pawn, int boardPosition, int pawnIndex) {
        Vector3 position = calculatePawnPosition(boardPosition);
        pawn.transform.setToTranslation(position);
        pawn.transform.scale(PAWN_SCALE, PAWN_SCALE, PAWN_SCALE);
        pawnPositions.put(pawnIndex, position);
    }


    public void updatePawnPosition(int pawnIndex, int newPosition) {
        if (pawnIndex >= 0 && pawnIndex < pawnInstances.size) {
            ModelInstance pawn = pawnInstances.get(pawnIndex);
            Vector3 newPos = calculatePawnPosition(newPosition);

            // Update transform and stored position
            pawn.transform.setToTranslation(newPos);
            pawn.transform.scale(PAWN_SCALE, PAWN_SCALE, PAWN_SCALE);
            pawnPositions.put(pawnIndex, newPos);
        }
    }


    public void dispose() {
        if (boardModel != null) boardModel.dispose();
        if (boardTexture != null) boardTexture.dispose();
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
        // Convert screen coordinates to 3D ray
        ray.set(camera.getPickRay(screenX, screenY));

        // Check intersection with each pawn's bounding sphere
        float minDist = Float.MAX_VALUE;
        int selectedPawn = -1;
        for (int i = 0; i < pawnInstances.size; i++) {
            Vector3 pawnPos = pawnPositions.get(i);
            if (pawnPos != null) {
                // Use direct sphere intersection test
                float radius = SQUARE_SIZE * 0.4f; // Adjust radius as needed
                if (Intersector.intersectRaySphere(ray, pawnPos, radius, intersection)) {
                    float dist = intersection.dst2(camera.position);
                    if (dist < minDist) {
                        minDist = dist;
                        selectedPawn = i;
                    }
                }
            }
        }

        return selectedPawn;
    }

    public void highlightPawn(int pawnIndex) {
        if (pawnIndex >= 0 && pawnIndex < pawnInstances.size) {
            ModelInstance pawn = pawnInstances.get(pawnIndex);
            // Add highlight effect (e.g., glow or outline)
            // This would require additional shader implementation
        }
    }

    public void updatePawnPositions() {
        // Update all pawn positions based on current state
        for (int i = 0; i < pawnInstances.size; i++) {
            ModelInstance pawn = pawnInstances.get(i);
            Vector3 position = pawnPositions.get(i);
            if (position != null) {
                pawn.transform.setToTranslation(position);
                pawn.transform.scale(PAWN_SCALE, PAWN_SCALE, PAWN_SCALE);
            }
        }
    }

    public void resize(int width, int height) {
        // Update any viewport-dependent calculations if needed
    }
}
