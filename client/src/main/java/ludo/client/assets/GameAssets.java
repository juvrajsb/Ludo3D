package ludo.client.assets;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.Texture;

public class GameAssets {
    private static GameAssets instance;
    private AssetManager assetManager;

    private GameAssets() {
        assetManager = new AssetManager();
        loadAssets();
    }

    public static GameAssets getInstance() {
        if (instance == null) {
            instance = new GameAssets();
        }
        return instance;
    }

    private void loadAssets() {
        loadModels();
        loadTextures();

        // Block until all assets are loaded
        assetManager.finishLoading();
    }

    private void loadTextures() {
        // Load textures
        assetManager.load("images/board.png", Texture.class);
    }

    private void loadModels() {
        // Load pawn model
        assetManager.load("models/pawn.g3db", Model.class);
        // Load dice model
        assetManager.load("models/dice.g3db", Model.class);
    }

    public Model getPawnModel() {
        return assetManager.get("models/pawn.g3db", Model.class);
    }

    public Model getDiceModel() {
        return assetManager.get("models/dice.g3db", Model.class);
    }

    public Texture getBoardTexture() {
        return assetManager.get("images/board.png", Texture.class);
    }

    public void dispose() {
        assetManager.dispose();
        instance = null;
    }
}
