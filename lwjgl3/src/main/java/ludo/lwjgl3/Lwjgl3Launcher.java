package ludo.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import ludo.client.LudoGame;

public class Lwjgl3Launcher {
    private static final int DEFAULT_WIDTH = 800;
    private static final int DEFAULT_HEIGHT = 600;
    private static final String GAME_TITLE = "Ludo Game";

    public static void main(String[] args) {
        try {
            if (StartupHelper.startNewJvmIfRequired()) return;
            createApplication();
        } catch (Exception e) {
            System.err.println("Error launching game: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static Lwjgl3Application createApplication() {
        try {
            return new Lwjgl3Application(new LudoGame(), getDefaultConfiguration());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create application", e);
        }
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();

        configuration.setTitle(GAME_TITLE);
        configuration.setWindowedMode(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        configuration.setWindowIcon("assets/icon.png");
        configuration.useVsync(true);
        configuration.setForegroundFPS(60);
        configuration.setResizable(true);

        return configuration;
    }
}
