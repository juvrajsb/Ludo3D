package ludo.client.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import ludo.client.assets.GameAssets;
import ludo.core.entities.*;

public class GameRenderer {
    private final PerspectiveCamera camera;
    private final ModelBatch modelBatch;
    private final Environment environment;
    private final ModelInstance boardInstance;
    private final ModelInstance[] pawns;
    private float rotationAngle = 0;
    private Model boardModel;
    private Texture boardTexture;

    public GameRenderer(int width, int height) {
        // Set up camera
        camera = new PerspectiveCamera(60, width, height);
        camera.position.set(0f, 10f, 10f); // Position camera above and behind the board
        camera.lookAt(0, 0, 0);
        camera.near = 0.1f;
        camera.far = 300f;
        camera.update();

        // Set up lighting
        modelBatch = new ModelBatch();
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1f));

        // Add directional lights from different angles
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
        environment.add(new DirectionalLight().set(0.2f, 0.2f, 0.2f, 1f, -0.8f, -0.2f));

        // Load board texture
        try {
            boardTexture = new Texture(Gdx.files.internal("images/board.png"));
            boardTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } catch (Exception e) {
            System.out.println("Failed to load board texture: " + e.getMessage());
            // Create a default colored plane if texture fails to load
            ModelBuilder modelBuilder = new ModelBuilder();
            boardModel = modelBuilder.createBox(8f, 0.1f, 8f,
                new Material(ColorAttribute.createDiffuse(Color.LIGHT_GRAY)),
                Usage.Position | Usage.Normal);
            boardInstance = new ModelInstance(boardModel);
            boardInstance.transform.translate(0, 0, 0);
            pawns = new ModelInstance[16];
            return;
        }

        // Create board model with texture
        ModelBuilder modelBuilder = new ModelBuilder();
        boardModel = modelBuilder.createRect(
            -4f, 0f, 4f,    // top left
            4f, 0f, 4f,     // top right
            4f, 0f, -4f,    // bottom right
            -4f, 0f, -4f,   // bottom left
            0f, 1f, 0f,     // normal (facing up)
            new Material(TextureAttribute.createDiffuse(boardTexture)),
            Usage.Position | Usage.Normal | Usage.TextureCoordinates
        );

        boardInstance = new ModelInstance(boardModel);
        pawns = new ModelInstance[16];

        // Add a debug grid to help with orientation
        addDebugGrid(modelBuilder);
    }

    private void addDebugGrid(ModelBuilder modelBuilder) {
        // Add debug grid lines
        Model gridModel = modelBuilder.createLineGrid(10, 10, 1f, 1f,
            new Material(ColorAttribute.createDiffuse(Color.GRAY)),
            Usage.Position | Usage.Normal);
        ModelInstance gridInstance = new ModelInstance(gridModel);
        gridInstance.transform.translate(0, -0.01f, 0); // Slightly below board
    }

    public void createPawnModels(Model pawnModel, Player[] players) {
        if (pawnModel == null) {
            System.err.println("Error: Pawn model not available!");
            return;
        }

        int index = 0;
        for (Player player : players) {
            for (Pawn pawn : player.getPawns()) {
                ModelInstance pawnInstance = new ModelInstance(pawnModel);
                // Apply player color to the pawn material
                pawnInstance.materials.get(0).set(getPawnColor(player.getColor()));
                // Scale the pawn to an appropriate size (adjust these values as needed)
                pawnInstance.transform.scale(0.5f, 0.5f, 0.5f);
                // Set initial position
                updatePawnPosition(index, pawn.getPosition());
                pawns[index++] = pawnInstance;
            }
        }
    }

    public void render() {
        // Set background color to light blue
        Gdx.gl.glClearColor(0.5f, 0.5f, 0.7f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // Update camera
        camera.update();

        modelBatch.begin(camera);

        // Render board
        modelBatch.render(boardInstance, environment);

        // Render pawns
        for (ModelInstance pawn : pawns) {
            if (pawn != null) {
                modelBatch.render(pawn, environment);
            }
        }

        modelBatch.end();
    }

    public void updatePawnPosition(int index, int position) {
        if (pawns[index] != null) {
            if (position == -1) {
                // Home position
                pawns[index].transform.setToTranslation(-3, 0.5f, -3);
            } else {
                float x = (position % 10) - 4f;
                float z = (position / 10) - 4f;
                pawns[index].transform.setToTranslation(x, 0.5f, z);
            }
        }
    }

    public void rotateCamera(float delta) {
        rotationAngle += delta;
        float radius = 12f;
        camera.position.set(
            (float) (radius * Math.cos(rotationAngle)),
            10f,
            (float) (radius * Math.sin(rotationAngle))
        );
        camera.lookAt(0, 0, 0);
        camera.up.set(Vector3.Y);
        camera.update();
    }

    private ColorAttribute getPawnColor(String playerColor) {
        switch (playerColor.toLowerCase()) {
            case "red": return ColorAttribute.createDiffuse(0.8f, 0.1f, 0.1f, 1);
            case "green": return ColorAttribute.createDiffuse(0.1f, 0.8f, 0.1f, 1);
            case "blue": return ColorAttribute.createDiffuse(0.1f, 0.1f, 0.8f, 1);
            case "yellow": return ColorAttribute.createDiffuse(0.8f, 0.8f, 0.1f, 1);
            default: return ColorAttribute.createDiffuse(1, 1, 1, 1);
        }
    }

    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }

    public void dispose() {
        modelBatch.dispose();
        if (boardModel != null) boardModel.dispose();
        if (boardTexture != null) boardTexture.dispose();
    }
}
