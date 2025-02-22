package ludo.core.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;

public class BoardCoordinates {
    // Constants for board dimensions and paths
    private static final int BOARD_SIZE = 15;  // 15x15 grid

    // Path positions for each color (clockwise movement)
    private static final int[][] MAIN_PATH = {
        // Bottom edge (0-12)
        {7, 14}, {7, 13}, {7, 12}, {7, 11}, {7, 10}, {7, 9}, {7, 8},
        {8, 8}, {9, 8}, {10, 8}, {11, 8}, {12, 8}, {13, 8},
        // Right edge (13-25)
        {13, 7}, {12, 7}, {11, 7}, {10, 7}, {9, 7}, {8, 7}, {7, 7},
        {7, 6}, {7, 5}, {7, 4}, {7, 3}, {7, 2}, {7, 1},
        // Top edge (26-38)
        {6, 1}, {6, 2}, {6, 3}, {6, 4}, {6, 5}, {6, 6}, {6, 7},
        {5, 7}, {4, 7}, {3, 7}, {2, 7}, {1, 7}, {0, 7},
        // Left edge (39-51)
        {0, 8}, {1, 8}, {2, 8}, {3, 8}, {4, 8}, {5, 8}, {6, 8}
    };

    private static final int[][][] HOME_BASES = {
        // YELLOW (0:6,0:6)
        {{0, 0}, {1, 0}, {0, 1}, {1, 1}},
        // BLUE (0:6,9:15)
        {{0, 9}, {1, 9}, {0, 10}, {1, 10}},
        // RED (9:15,9:15)
        {{9, 9}, {10, 9}, {9, 10}, {10, 10}},
        // GREEN (9:15,0:6)
        {{9, 0}, {10, 0}, {9, 1}, {10, 1}}
    };

    // Home column paths
    private static final int[][][] HOME_COLUMNS = {
        // RED (goes up)
        {{7, 9}, {7, 10}, {7, 11}, {7, 12}, {7, 13}, {7, 14}},
        // BLUE (goes left)
        {{12, 7}, {11, 7}, {10, 7}, {9, 7}, {8, 7}, {7, 7}},
        // GREEN (goes right)
        {{1, 7}, {2, 7}, {3, 7}, {4, 7}, {5, 7}, {6, 7}},
        // YELLOW (goes down)
        {{7, 5}, {7, 4}, {7, 3}, {7, 2}, {7, 1}, {7, 0}}
    };

    public static Vector3 getMainPathPosition(int position) {
        position = position % MAIN_PATH.length;
        int[] coords = MAIN_PATH[position];
        return gridToWorld(coords[0], coords[1]);
    }

    public static String worldToGrid(Vector3 worldPosition) {
        int gridX = Math.round(worldPosition.x / 0.5f + (BOARD_SIZE / 2));
        int gridZ = Math.round((BOARD_SIZE / 2) - worldPosition.z / 0.5f);
        return String.format("(%d, %d)", gridX, gridZ);
    }

    public static Vector3 getHomeBasePosition(String color, int index) {
        int colorIndex = getColorIndex(color);

        // Log the lookup process
        Gdx.app.log("BoardCoordinates", String.format(
            "getHomeBasePosition lookup: color=%s, colorIndex=%d, index=%d",
            color, colorIndex, index
        ));

        int[] coords = HOME_BASES[colorIndex][index];

        // Log the result coordinates
        Gdx.app.log("BoardCoordinates", String.format(
            "HOME_BASES lookup result: [%d,%d]",
            coords[0], coords[1]
        ));

        return gridToWorld(coords[0], coords[1]);
    }

    public static Vector3 getHomeColumnPosition(String color, int step) {
        int colorIndex = getColorIndex(color);
        int[] coords = HOME_COLUMNS[colorIndex][step];
        return gridToWorld(coords[0], coords[1]);
    }

    public static int[] getGridCoordsForPosition(int boardPosition) {
        if (boardPosition < 0 || boardPosition >= MAIN_PATH.length) {
            return null;
        }
        return MAIN_PATH[boardPosition];
    }

    private static int getColorIndex(String color) {
        switch(color.toUpperCase()) {
            case "YELLOW": return 0;  // Maps to first array (0:6,0:6)
            case "BLUE": return 1;    // Maps to second array (0:6,9:15)
            case "RED": return 2;     // Maps to third array (9:15,9:15)
            case "GREEN": return 3;   // Maps to fourth array (9:15,0:6)
            default: throw new IllegalArgumentException("Invalid color: " + color);
        }
    }

    public static Vector3 gridToWorld(int x, int y) {
        float worldX = (y - (float) BOARD_SIZE /2);
        float worldZ = (x - (float) BOARD_SIZE /2);

        Gdx.app.log("BoardCoordinates", String.format(
            "gridToWorld: input(%d,%d) -> output(%.2f,%.2f,%.2f)",
            x, y, worldX, 0.0f, worldZ
        ));

        return new Vector3(worldX, 0, worldZ);
    }
}
