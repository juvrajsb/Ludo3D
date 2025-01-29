package ludo.client.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import ludo.core.entities.Pawn;
import ludo.core.entities.Player;
/**
 * This class is responsible for rendering the board.
 */
public class BoardRenderer {
    private static final int CELL_SIZE = 40;
    private static final int BOARD_SIZE = 15; // 15x15 grid
    private static final int PAWN_RADIUS = 15;
    private ShapeRenderer shapeRenderer;

    public BoardRenderer() {
        shapeRenderer = new ShapeRenderer();
    }

    public void render(Player[] players) {
        shapeRenderer.begin(ShapeType.Filled);

        // Draw board grid
        drawGrid();

        // Draw pawns for each player
        for (Player player : players) {
            if (player != null) {
                drawPlayerPawns(player);
            }
        }

        shapeRenderer.end();
    }

    private void drawGrid() {
        // Draw white background
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(0, 0, BOARD_SIZE * CELL_SIZE, BOARD_SIZE * CELL_SIZE);

        // Draw grid lines
        shapeRenderer.setColor(Color.BLACK);
        for (int i = 0; i <= BOARD_SIZE; i++) {
            // Vertical lines
            shapeRenderer.line(i * CELL_SIZE, 0, i * CELL_SIZE, BOARD_SIZE * CELL_SIZE);
            // Horizontal lines
            shapeRenderer.line(0, i * CELL_SIZE, BOARD_SIZE * CELL_SIZE, i * CELL_SIZE);
        }
    }

    private void drawPlayerPawns(Player player) {
        Color pawnColor = getColorFromString(player.getColor());
        shapeRenderer.setColor(pawnColor);

        for (Pawn pawn : player.getPawns()) {
            int position = pawn.getPosition();
            if (position >= 0) { // Only draw pawns that are on the board
                // Convert board position to screen coordinates
                float x = (position % BOARD_SIZE) * CELL_SIZE + CELL_SIZE/2;
                float y = (position / BOARD_SIZE) * CELL_SIZE + CELL_SIZE/2;
                shapeRenderer.circle(x, y, PAWN_RADIUS);
            }
        }
    }

    private Color getColorFromString(String colorStr) {
        switch (colorStr.toLowerCase()) {
            case "red": return Color.RED;
            case "blue": return Color.BLUE;
            case "green": return Color.GREEN;
            case "yellow": return Color.YELLOW;
            default: return Color.WHITE;
        }
    }

    public void dispose() {
        shapeRenderer.dispose();
    }
}
