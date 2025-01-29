package ludo.client.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
/**
 * This class is responsible for rendering the dice.
 */
public class DiceRenderer {
    private ShapeRenderer shapeRenderer;
    private static final float DICE_SIZE = 50f;
    private static final float DOT_RADIUS = 5f;
    private Rectangle bounds;
    private int currentValue;

    public DiceRenderer() {
        shapeRenderer = new ShapeRenderer();
        bounds = new Rectangle();
    }

    public void render(int value, float x, float y) {
        currentValue = value;
        bounds.set(x, y, DICE_SIZE, DICE_SIZE);

        // Draw dice outline
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(x, y, DICE_SIZE, DICE_SIZE);

        // Draw border
        shapeRenderer.setColor(Color.BLACK);
        shapeRenderer.rect(x, y, DICE_SIZE, DICE_SIZE); //TODO: check

        // Draw dots based on value
        shapeRenderer.setColor(Color.BLACK);
        drawDots(value, x, y);

        shapeRenderer.end();
    }

    private void drawDots(int value, float x, float y) {
        switch (value) {
            case 1:
                drawDot(x + DICE_SIZE/2, y + DICE_SIZE/2);
                break;
            case 2:
                drawDot(x + DICE_SIZE/4, y + DICE_SIZE/4);
                drawDot(x + 3*DICE_SIZE/4, y + 3*DICE_SIZE/4);
                break;
            case 3:
                drawDot(x + DICE_SIZE/4, y + DICE_SIZE/4);
                drawDot(x + DICE_SIZE/2, y + DICE_SIZE/2);
                drawDot(x + 3*DICE_SIZE/4, y + 3*DICE_SIZE/4);
                break;
            case 4:
                drawDot(x + DICE_SIZE/4, y + DICE_SIZE/4);
                drawDot(x + 3*DICE_SIZE/4, y + DICE_SIZE/4);
                drawDot(x + DICE_SIZE/4, y + 3*DICE_SIZE/4);
                drawDot(x + 3*DICE_SIZE/4, y + 3*DICE_SIZE/4);
                break;
            case 5:
                drawDot(x + DICE_SIZE/4, y + DICE_SIZE/4);
                drawDot(x + 3*DICE_SIZE/4, y + DICE_SIZE/4);
                drawDot(x + DICE_SIZE/2, y + DICE_SIZE/2);
                drawDot(x + DICE_SIZE/4, y + 3*DICE_SIZE/4);
                drawDot(x + 3*DICE_SIZE/4, y + 3*DICE_SIZE/4);
                break;
            case 6:
                drawDot(x + DICE_SIZE/4, y + DICE_SIZE/4);
                drawDot(x + 3*DICE_SIZE/4, y + DICE_SIZE/4);
                drawDot(x + DICE_SIZE/4, y + DICE_SIZE/2);
                drawDot(x + 3*DICE_SIZE/4, y + DICE_SIZE/2);
                drawDot(x + DICE_SIZE/4, y + 3*DICE_SIZE/4);
                drawDot(x + 3*DICE_SIZE/4, y + 3*DICE_SIZE/4);
                break;
        }
    }

    private void drawDot(float x, float y) {
        shapeRenderer.circle(x, y, DOT_RADIUS);
    }

    public boolean isClicked(float x, float y) {
        // Convert y coordinate since LibGDX uses bottom-left corner as origin
        float flippedY = com.badlogic.gdx.Gdx.graphics.getHeight() - y;
        return bounds.contains(x, flippedY);
    }

    public void updateValue(int value) {
        this.currentValue = value;
    }

    public void dispose() {
        shapeRenderer.dispose();
    }
}
