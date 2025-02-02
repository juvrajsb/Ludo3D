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

public class GameRenderer {
    private Model boardModel;
    private ModelInstance boardInstance;
    private Array<ModelInstance> pawnInstances;
    private Map<Integer, Vector3> pawnPositions;
    private Map<String, Color> playerColors;
    private final float BOARD_SIZE = 8f;
    private final float SQUARE_SIZE = BOARD_SIZE / 15f; // 15x15 grid
    private final float PAWN_SCALE = 0.15f;
    private final Ray ray;
    private final Vector3 intersection;
    private Texture boardTexture;

    public GameRenderer(int width, int height) {
        pawnInstances = new Array<>();
        pawnPositions = new HashMap<>();
        ray = new Ray();
        intersection = new Vector3();
        initializePlayerColors();
        createBoard();
    }

    private void initializePlayerColors() {
        playerColors = new HashMap<>();
        playerColors.put("RED", Color.RED);
        playerColors.put("GREEN", Color.GREEN);
        playerColors.put("BLUE", Color.BLUE);
        playerColors.put("YELLOW", Color.YELLOW);
    }

    private void createBoard() {
        // Load board texture
        boardTexture = new Texture(Gdx.files.internal("board.png"));
        boardTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        ModelBuilder modelBuilder = new ModelBuilder();

        // Create board model with UV coordinates for texture mapping
        boardModel = modelBuilder.createBox(
            BOARD_SIZE, 0.2f, BOARD_SIZE,
            new Material(TextureAttribute.createDiffuse(boardTexture)),
            Usage.Position | Usage.Normal | Usage.TextureCoordinates
        );

        boardInstance = new ModelInstance(boardModel);
        boardInstance.transform.translate(0, -0.1f, 0); // Slightly below pawns
    }

    public void createPawns(List<Player> players) {
        pawnInstances.clear();
        pawnPositions.clear();

        Model pawnModel = GameAssets.getInstance().getPawnModel();
        int pawnIndex = 0;

        for (Player player : players) {
            Color playerColor = playerColors.get(player.getColor());
            Material pawnMaterial = new Material(ColorAttribute.createDiffuse(playerColor));

            for (int i = 0; i < player.getPawns().size(); i++) {
                ModelInstance pawnInstance = new ModelInstance(pawnModel);
                pawnInstance.materials.get(0).set(pawnMaterial);

                // Scale and position the pawn
                pawnInstance.transform.scale(PAWN_SCALE, PAWN_SCALE, PAWN_SCALE);
                positionPawn(pawnInstance, player.getPawns().get(i).getPosition(), pawnIndex);

                pawnInstances.add(pawnInstance);
                pawnIndex++;
            }
        }
    }

    private void positionPawn(ModelInstance pawn, int boardPosition, int pawnIndex) {
        Vector3 position = calculatePawnPosition(boardPosition);
        pawn.transform.setTranslation(position);
        pawnPositions.put(pawnIndex, position);
    }

    private Vector3 calculatePawnPosition(int boardPosition) {
        if (boardPosition == -1) { // Pawn in home
            return new Vector3(-BOARD_SIZE/2 + SQUARE_SIZE, 0.2f, -BOARD_SIZE/2 + SQUARE_SIZE);
        }

        // Calculate grid position
        float x = (boardPosition % 15 - 7) * SQUARE_SIZE;
        float z = (boardPosition / 15 - 7) * SQUARE_SIZE;
        return new Vector3(x, 0.2f, z);
    }

    public void render(ModelBatch modelBatch, Environment environment) {
        // Render board
        modelBatch.render(boardInstance, environment);

        // Render pawns
        for (ModelInstance pawn : pawnInstances) {
            modelBatch.render(pawn, environment);
        }
    }

    public void updatePawnPosition(int pawnIndex, int newPosition) {
        if (pawnIndex >= 0 && pawnIndex < pawnInstances.size) {
            Vector3 newPos = calculatePawnPosition(newPosition);
            ModelInstance pawn = pawnInstances.get(pawnIndex);

            // Update transform and stored position
            pawn.transform.setToTranslation(newPos);
            pawn.transform.scale(PAWN_SCALE, PAWN_SCALE, PAWN_SCALE);
            pawnPositions.put(pawnIndex, newPos);
        }
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

    public void resize(int width, int height) {
        // Update any viewport-dependent calculations if needed
    }

    public void dispose() {
        if (boardModel != null) boardModel.dispose();
        if (boardTexture != null) boardTexture.dispose();
    }
}
