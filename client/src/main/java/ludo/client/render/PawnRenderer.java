package ludo.client.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ludo.core.entities.Pawn;
import ludo.core.entities.Player;

public class PawnRenderer {
    private final ShapeRenderer shapeRenderer;
    private static final float CELL_SIZE = 40f;
    private static final float PAWN_RADIUS = 15f;
    private Player currentPlayer;

    public PawnRenderer() {
        this.shapeRenderer = new ShapeRenderer();
        this.currentPlayer = currentPlayer;


    }

    public void render(Player player) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        drawPlayerPawns(player);
        shapeRenderer.end();
    }

    private void drawPlayerPawns(Player player) {
        Color pawnColor = getColorFromString(player.getColor());
        shapeRenderer.setColor(pawnColor);

        for (Pawn pawn : player.getPawns()) {
            int position = pawn.getPosition();
            if (position >= 0) { // Only draw pawns that are on the board
                // Convert board position to screen coordinates
                float x = (position % 15) * CELL_SIZE + CELL_SIZE/2;
                float y = (position / 15) * CELL_SIZE + CELL_SIZE/2;
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

    public int getPawnAtPosition(float x, float y) {
        for (int i = 0; i < currentPlayer.getPawns().size(); i++) {
            Pawn pawn = currentPlayer.getPawns().get(i);
            int position = pawn.getPosition();
            if (position >= 0) {
                float pawnX = (position % 15) * CELL_SIZE + CELL_SIZE / 2;
                float pawnY = (position / 15) * CELL_SIZE + CELL_SIZE / 2;
                float dx = x - pawnX;
                float dy = y - pawnY;
                if (dx * dx + dy * dy <= PAWN_RADIUS * PAWN_RADIUS) {
                    return i;
                }
            }
        }
        return -1;
    }
}
