package ludo.client.render;

import com.badlogic.gdx.graphics.OrthographicCamera;

/**
 * This class is responsible for the camera.
 */
public class GameCamera extends OrthographicCamera {
    private static final float VIEWPORT_WIDTH = 800;
    private static final float VIEWPORT_HEIGHT = 600;

    public GameCamera() {
        super(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        position.set(VIEWPORT_WIDTH / 2, VIEWPORT_HEIGHT / 2, 0);
        update();
    }
}
