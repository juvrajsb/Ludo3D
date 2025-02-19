package ludo.client.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector3;

class GameInputProcessor extends InputAdapter {
    private static final float DRAG_THRESHOLD = 5f;
    private final GameScreen gameScreen;
    private final Vector3 touchPoint = new Vector3();
    private final boolean isMacOS = System.getProperty("os.name").toLowerCase().contains("mac");
    private boolean isDragging = false;
    private float startX, startY;

    public GameInputProcessor(GameScreen gameScreen) {
        this.gameScreen = gameScreen;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        // Map button codes for macOS
        int mappedButton = button;
        if (isMacOS) {
            switch (button) {
                case 0:
                    mappedButton = Input.Buttons.LEFT;
                    break;
                case 1:
                    mappedButton = Input.Buttons.RIGHT;
                    break;
            }
        }

        if (mappedButton != Input.Buttons.LEFT) {
            return false;
        }

        startX = screenX;
        startY = screenY;
        isDragging = false;

        touchPoint.set(screenX, screenY, 0);
        gameScreen.camera.unproject(touchPoint);

        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        // Map button codes for macOS
        int mappedButton = isMacOS ? (button == 0 ? Input.Buttons.LEFT : button) : button;

        if (mappedButton != Input.Buttons.LEFT) {
            return false;
        }

        float dx = Math.abs(screenX - startX);
        float dy = Math.abs(screenY - startY);
        boolean wasDrag = (dx * dx + dy * dy) > DRAG_THRESHOLD * DRAG_THRESHOLD;

        if (!wasDrag && !isDragging) {
            touchPoint.set(screenX, screenY, 0);
            gameScreen.camera.unproject(touchPoint);

            gameScreen.handlePawnSelection(screenX, screenY);
        }

        isDragging = false;
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (!isDragging) {
            float dx = Math.abs(screenX - startX);
            float dy = Math.abs(screenY - startY);
            isDragging = (dx * dx + dy * dy) > DRAG_THRESHOLD * DRAG_THRESHOLD;
        }

        if (isDragging) {
            float deltaX = (screenX - startX) * 0.5f;
            float deltaY = (screenY - startY) * 0.5f;

            gameScreen.cameraRotation += deltaX * 0.2f;
            gameScreen.cameraHeight = Math.max(gameScreen.minHeight, Math.min(gameScreen.maxHeight, gameScreen.cameraHeight + deltaY * 0.1f));

            gameScreen.updateCameraPosition();
            startX = screenX;
            startY = screenY;
        }

        return true;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        float newDistance = gameScreen.cameraDistance + amountY * gameScreen.zoomSpeed;
        newDistance = Math.max(gameScreen.minZoom, Math.min(gameScreen.maxZoom, newDistance));

        if (newDistance != gameScreen.cameraDistance) {
            gameScreen.cameraDistance = newDistance;
            gameScreen.updateCameraPosition();
        }

        return true;
    }
}
