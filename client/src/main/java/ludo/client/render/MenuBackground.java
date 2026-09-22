package ludo.client.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import ludo.client.assets.GameAssets;
import ludo.core.utils.BoardCoordinates;

import java.util.HashMap;
import java.util.Map;

/**
 * Renders an animated, cinematic 3D showcase of the Ludo board, table, dice, and pawns
 * inside the scenic outdoor skybox. Provides an ambient background for all menu screens.
 */
public class MenuBackground implements Disposable {
    private static final String TAG = "MenuBackground";
    private static final float BOARD_SIZE = 15f;
    private static final float PAWN_SCALE = 7f;

    private final PerspectiveCamera camera;
    private final ModelBatch modelBatch;
    private final Environment environment;
    private Skybox skybox;
    private final ShapeRenderer overlayRenderer;

    private Model boardModel;
    private Texture boardTexture;
    private ModelInstance boardInstance;
    private ModelInstance tableInstance;
    private ModelInstance diceInstance;
    private final Array<ModelInstance> pawnInstances = new Array<>();

    private float cameraAngle = 0f;
    private final float cameraDistance = 15.5f;
    private final float cameraHeight = 9.0f;
    private final Vector3 cameraTarget = new Vector3(0, 0, 0);

    public MenuBackground() {
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();

        camera = new PerspectiveCamera(65, width > 0 ? width : 1280, height > 0 ? height : 720);
        camera.near = 1f;
        camera.far = 300f;
        updateCameraPosition();

        modelBatch = new ModelBatch();
        environment = new Environment();
        setupLighting();

        overlayRenderer = new ShapeRenderer();

        try {
            skybox = new Skybox();
        } catch (Exception e) {
            Gdx.app.error(TAG, "Failed to load skybox: " + e.getMessage());
            skybox = null;
        }

        createSceneModels();
    }

    private void setupLighting() {
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.45f, 0.45f, 0.45f, 1f));
        DirectionalLight mainLight = new DirectionalLight().set(0.9f, 0.9f, 0.85f, -1f, -0.8f, -0.3f);
        environment.add(mainLight);

        DirectionalLight fillLight = new DirectionalLight().set(0.35f, 0.35f, 0.4f, 1f, -0.4f, 0.3f);
        environment.add(fillLight);
    }

    private void createSceneModels() {
        // 1. Table
        try {
            Model tableModel = GameAssets.getInstance().getTableModel();
            if (tableModel != null) {
                tableInstance = new ModelInstance(tableModel);
                tableInstance.transform.translate(0, -14.5f, 0);
                Color brownTint = new Color(0.5f, 0.4f, 0.35f, 1f);
                for (Material mat : tableInstance.materials) {
                    mat.remove(ColorAttribute.Emissive);
                    mat.set(ColorAttribute.createDiffuse(brownTint));
                    mat.remove(ColorAttribute.Specular);
                }
            }
        } catch (Exception e) {
            Gdx.app.error(TAG, "Error initializing table: " + e.getMessage());
        }

        // 2. Board
        try {
            boardTexture = new Texture(Gdx.files.internal("images/board.png"));
            boardTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

            ModelBuilder modelBuilder = new ModelBuilder();
            Material material = new Material(TextureAttribute.createDiffuse(boardTexture));
            boardModel = modelBuilder.createBox(
                BOARD_SIZE,
                0.1f,
                BOARD_SIZE,
                material,
                Usage.Position | Usage.Normal | Usage.TextureCoordinates
            );
            boardInstance = new ModelInstance(boardModel);
            boardInstance.transform.setToTranslation(0, 0, 0);
        } catch (Exception e) {
            Gdx.app.error(TAG, "Error initializing board: " + e.getMessage());
        }

        // 3. Pawns (4 for each color on their home bases)
        try {
            Model pawnModel = GameAssets.getInstance().getPawnModel();
            if (pawnModel != null) {
                Map<String, Color> colors = new HashMap<>();
                colors.put("YELLOW", new Color(0.96f, 0.77f, 0.19f, 1f));
                colors.put("BLUE", new Color(0.18f, 0.58f, 0.92f, 1f));
                colors.put("RED", new Color(0.92f, 0.26f, 0.21f, 1f));
                colors.put("GREEN", new Color(0.24f, 0.70f, 0.29f, 1f));

                for (Map.Entry<String, Color> entry : colors.entrySet()) {
                    String colorName = entry.getKey();
                    Color color = entry.getValue();

                    Material mat = new Material(
                        ColorAttribute.createDiffuse(color),
                        ColorAttribute.createSpecular(1f, 1f, 1f, 1f),
                        ColorAttribute.createAmbient(color.r * 0.4f, color.g * 0.4f, color.b * 0.4f, 1f)
                    );

                    for (int i = 0; i < 4; i++) {
                        ModelInstance pawnInstance = new ModelInstance(pawnModel);
                        for (Material m : pawnInstance.materials) {
                            m.clear();
                            m.set(mat);
                        }

                        Vector3 basePos = BoardCoordinates.getHomeBasePosition(colorName, i);
                        pawnInstance.transform.setToTranslation(basePos);
                        pawnInstance.transform.scale(PAWN_SCALE, PAWN_SCALE, PAWN_SCALE);
                        pawnInstances.add(pawnInstance);
                    }
                }
            }
        } catch (Exception e) {
            Gdx.app.error(TAG, "Error initializing pawns: " + e.getMessage());
        }

        // 4. Center Dice
        try {
            Model diceModel = GameAssets.getInstance().getDiceModel();
            if (diceModel != null) {
                diceInstance = new ModelInstance(diceModel);
                diceInstance.transform.setToTranslation(0, 0.45f, 0);
                diceInstance.transform.scale(0.85f, 0.85f, 0.85f);
                diceInstance.transform.rotate(Vector3.Y, 30f);
                diceInstance.transform.rotate(Vector3.X, 15f);
            }
        } catch (Exception e) {
            Gdx.app.error(TAG, "Error initializing dice: " + e.getMessage());
        }
    }

    private void updateCameraPosition() {
        double rad = Math.toRadians(cameraAngle);
        float x = (float) (cameraDistance * Math.cos(rad));
        float z = (float) (cameraDistance * Math.sin(rad));

        camera.position.set(x, cameraHeight, z);
        camera.lookAt(cameraTarget);
        camera.up.set(Vector3.Y);
        camera.update();
    }

    public void render(float delta) {
        // Slowly orbit camera
        cameraAngle += delta * 5.0f;
        if (cameraAngle >= 360f) {
            cameraAngle -= 360f;
        }
        updateCameraPosition();

        // 1. Render Skybox
        if (skybox != null) {
            skybox.render(camera);
        }

        // 2. Render 3D Scene
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        modelBatch.begin(camera);

        if (tableInstance != null) {
            modelBatch.render(tableInstance, environment);
        }
        if (boardInstance != null) {
            modelBatch.render(boardInstance, environment);
        }
        for (ModelInstance pawn : pawnInstances) {
            if (pawn != null) {
                modelBatch.render(pawn, environment);
            }
        }
        if (diceInstance != null) {
            modelBatch.render(diceInstance, environment);
        }

        modelBatch.end();
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);

        // 3. Render semi-transparent dark cinematic scrim so foreground UI is readable
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        overlayRenderer.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        overlayRenderer.begin(ShapeRenderer.ShapeType.Filled);
        // Slightly darker at top/bottom for subtle vignette
        overlayRenderer.setColor(0.06f, 0.08f, 0.12f, 0.65f);
        overlayRenderer.rect(0, 0, width, height);
        overlayRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public void resize(int width, int height) {
        if (width > 0 && height > 0) {
            camera.viewportWidth = width;
            camera.viewportHeight = height;
            camera.update();
        }
    }

    @Override
    public void dispose() {
        if (skybox != null) {
            skybox.dispose();
        }
        if (modelBatch != null) {
            modelBatch.dispose();
        }
        if (boardModel != null) {
            boardModel.dispose();
        }
        if (boardTexture != null) {
            boardTexture.dispose();
        }
        if (overlayRenderer != null) {
            overlayRenderer.dispose();
        }
    }
}
