package ludo.android;

import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.Game;

public class AndroidLauncher extends AndroidApplication {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;
        config.useImmersiveMode = true;

        // Initialize with a launcher that will load the actual game
        initialize(new AndroidGameLauncher(), config);
    }

    // This class acts as a bridge to the actual game
    public static class AndroidGameLauncher extends Game {
        @Override
        public void create() {
            try {
                // Try to dynamically load the LudoGame class at runtime
                Class<?> ludoGameClass = Class.forName("ludo.client.LudoGame");
                Game actualGame = (Game) ludoGameClass.getDeclaredConstructor().newInstance();
                setScreen(actualGame.getScreen());
            } catch (Exception e) {
                throw new RuntimeException("Failed to load game: " + e.getMessage(), e);
            }
        }
    }
}
