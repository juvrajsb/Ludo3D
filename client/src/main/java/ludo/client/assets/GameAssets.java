package ludo.client.assets;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g3d.Model;

/**
 * Singleton class to manage game assets and provide access to loaded resources
 */
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
        // Load 3D models
        assetManager.load("models/board.g3db", Model.class);
        assetManager.load("models/pawn.g3db", Model.class);
        assetManager.load("models/dice.g3db", Model.class);

        // Block until all assets are loaded
        assetManager.finishLoading();
    }

    public Model getBoardModel() {
        return assetManager.get("models/board.g3db", Model.class);
    }

    public Model getPawnModel() {
        return assetManager.get("models/pawn.g3db", Model.class);
    }

    public Model getDiceModel() {
        return assetManager.get("models/dice.g3db", Model.class);
    }

    public void dispose() {
        assetManager.dispose();
        instance = null;
    }
}

