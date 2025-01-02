package ludo.client.assets;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g3d.Model;

public class AssetLoader {
    private static AssetManager manager;

    public static void init() {
        manager = new AssetManager();
        loadModels();
        loadTextures();
    }

    private static void loadModels() {
        manager.load("models/board.g3db", Model.class);
        manager.load("models/pawn.g3db", Model.class);
        manager.load("models/dice.g3db", Model.class);
    }

    private static void loadTextures() {
        // Load textures here
    }
}
