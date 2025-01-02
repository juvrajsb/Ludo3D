package ludo.client.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class DiceRenderer {
    private ShapeRenderer shapeRenderer;
    private static final float DICE_SIZE = 50f;
    private static final float DOT_RADIUS = 5f;

    public DiceRenderer() {
        shapeRenderer = new ShapeRenderer();
    }

    public void render(int value, float x, float y) {
        // Draw dice outline
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(x, y, DICE_SIZE, DICE_SIZE);

        // Draw border
        shapeRenderer.setColor(Color.BLACK);
        shapeRenderer.rect(x, y, DICE_SIZE, DICE_SIZE, 2);

        // Draw dots
        shapeRenderer.setColor(Color.BLACK);
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
        shapeRenderer.end();
    }

    private void drawDot(float x, float y) {
        shapeRenderer.circle(x, y, DOT_RADIUS);
    }

    public void dispose() {
        shapeRenderer.dispose();
    }
}
