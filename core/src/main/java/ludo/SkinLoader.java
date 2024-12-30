package ludo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class SkinLoader {
    public static Skin createSkin() {
        // Load the texture atlas first
        TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("ui/uiskin.atlas"));

        // Create the skin with the atlas
        Skin skin = new Skin(atlas);

        // Add just the font
//        skin.add("default", new BitmapFont(Gdx.files.internal("ui/default.fnt")));

        // Load the JSON file
        skin.load(Gdx.files.internal("ui/uiskin.json"));

        return skin;
    }
}
