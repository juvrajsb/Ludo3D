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
            Lwjgl3Application app = createApplication();
        } catch (Exception e) {
            System.err.println("Failed to create application: ");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Lwjgl3Application createApplication() {
        Lwjgl3ApplicationConfiguration config = getDefaultConfiguration();
        return new Lwjgl3Application(new LudoGame(), config);
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();

        configuration.setTitle(GAME_TITLE);
        configuration.setWindowedMode(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate);
        configuration.setWindowIcon("ui/icon.png", "ui/icon.png", "ui/icon.png", "ui/icon.png");
        configuration.useVsync(true);
        configuration.setResizable(true);

        configuration.setIdleFPS(60);
        configuration.setInitialVisible(true);
        configuration.setDecorated(true);

        return configuration;
    }
}
