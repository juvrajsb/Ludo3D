package ludo.core.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;

public class BoardCoordinates {
    private static final int BOARD_SIZE = 15;  // 15x15 grid

    private static final int[][] MAIN_PATH = {
        // Starting from Yellow's start position (moving clockwise)
        // Bottom track moving right
        {6, 1}, {6, 2}, {6, 3}, {6, 4}, {6, 5},
        // Bottom-right corner and up
        {5, 6}, {4, 6}, {3, 6}, {2, 6}, {1, 6}, {0, 6},
        // Left edge moving up
        {0, 7}, {0, 8},
        // Top-left corner and right (Blue's section)
        {1, 8}, {2, 8}, {3, 8}, {4, 8}, {5, 8},
        // Moving up into middle section
        {6, 9}, {6, 10}, {6, 11}, {6, 12}, {6, 13}, {6, 14},
        // Top edge moving right
        {7, 14}, {8, 14},
        // Top-right corner and down (Red's section)
        {8, 13}, {8, 12}, {8, 11}, {8, 10}, {8, 9},
        // Moving right into middle section
        {9, 8}, {10, 8}, {11, 8}, {12, 8}, {13, 8}, {14, 8},
        // Right edge moving down
        {14, 7}, {14, 6},
        // Bottom-right corner and left (Green's section)
        {13, 6}, {12, 6}, {11, 6}, {10, 6}, {9, 6},
        // Moving down into middle section
        {8, 5}, {8, 4}, {8, 3}, {8, 2}, {8, 1}, {8, 0},
        // Bottom edge moving left (back to Yellow's section)
        {7, 0}, {6, 0}
    };

    private static final int[][][] HOME_BASES = {
        // YELLOW (0:6,0:6)
        {{2, 2}, {2, 4}, {4, 2}, {4, 4}},
        // BLUE (0:6,9:15)
        {{2, 11}, {4, 11}, {2, 13}, {4, 13}},
        // RED (9:15,9:15)
        {{11, 11}, {11, 13}, {13, 11}, {13, 13}},
        // GREEN (9:15,0:6)
        {{11, 2}, {13, 2}, {11, 4}, {13, 4}}
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

//    public static String worldToGrid(Vector3 worldPosition) {
//        int gridX = Math.round(worldPosition.x / 0.5f + (BOARD_SIZE / 2));
//        int gridZ = Math.round((BOARD_SIZE / 2) - worldPosition.z / 0.5f);
//        return String.format("(%d, %d)", gridX, gridZ);
//    }

    public static Vector3 getHomeBasePosition(String color, int index) {
        int colorIndex = getColorIndex(color);
        int[] coords = HOME_BASES[colorIndex][index];
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

        return new Vector3(worldX, 0, worldZ);
    }
}
