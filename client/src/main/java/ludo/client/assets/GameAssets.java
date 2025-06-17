package ludo.client.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.utils.UBJsonReader;

public class GameAssets {
    private static final String TAG = "GameAssets";
    private static GameAssets instance;
    private Model pawnModel;
    private Model diceModel;
    private Texture pawnBaseColorTexture;
    private Texture pawnNormalTexture;
    private Texture pawnRoughnessTexture;

    private GameAssets() {
        Gdx.app.log(TAG, "Initializing GameAssets");
        loadAssets();
    }

    public static GameAssets getInstance() {
        if (instance == null) {
            instance = new GameAssets();
        }
        return instance;
    }

    private void loadAssets() {
        try {
            Gdx.app.log(TAG, "Loading game assets...");

            G3dModelLoader modelLoader = new G3dModelLoader(new UBJsonReader());
            try {
                pawnModel = modelLoader.loadModel(Gdx.files.internal("models/pawn.g3db"));
                Gdx.app.log(TAG, "Pawn model loaded successfully");
            } catch (Exception e) {
                Gdx.app.error(TAG, "Failed to load pawn model: " + e.getMessage());
                e.printStackTrace();
            }

            try {
                diceModel = modelLoader.loadModel(Gdx.files.internal("models/dice.g3db"));
                Gdx.app.log(TAG, "Dice model loaded successfully");
            } catch (Exception e) {
                Gdx.app.error(TAG, "Failed to load dice model: " + e.getMessage());
                e.printStackTrace();
            }

            // Load textures
            try {
                pawnBaseColorTexture = new Texture(Gdx.files.internal("models/textures/Carpet_BaseColor.jpg"));
                pawnNormalTexture = new Texture(Gdx.files.internal("models/textures/Carpet_Normal.jpg"));
                pawnRoughnessTexture = new Texture(Gdx.files.internal("models/textures/Carpet_Roughness.jpg"));

                pawnBaseColorTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                pawnNormalTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                pawnRoughnessTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

                Gdx.app.log(TAG, "Textures loaded successfully");
            } catch (Exception e) {
                Gdx.app.error(TAG, "Failed to load textures: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (Exception e) {
            Gdx.app.error(TAG, "Error loading assets: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Model getPawnModel() {
        if (pawnModel == null) {
            Gdx.app.error(TAG, "Pawn model is null!");
        }
        return pawnModel;
    }

    public Model getDiceModel() {
        return diceModel;
    }
}
