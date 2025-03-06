package ludo.android;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import ludo.client.LudoGame;

public class AndroidLauncher extends AndroidApplication {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();

        // Disable screen dimming
        config.useWakelock = true;

        // Enable hardware acceleration
        config.useAccelerometer = false;
        config.useCompass = false;

        // Handle different screen sizes
        config.useImmersiveMode = true; // Hide system UI for fullscreen

        // Initialize the game
        initialize(new LudoGame(), config);
    }
}
