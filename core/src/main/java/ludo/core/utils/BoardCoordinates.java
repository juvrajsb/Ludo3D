package ludo.core.utils;

import com.badlogic.gdx.math.Vector3;

public class BoardCoordinates {
    // Constants for board dimensions and paths
    private static final int BOARD_SIZE = 15;  // 15x15 grid
    private static final float CELL_SIZE = 0.5f;  // Size of each cell in world units

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

    // Home base positions for each color
    private static final int[][][] HOME_BASES = {
        // RED (bottom right)
        {{13, 13}, {14, 13}, {13, 14}, {14, 14}},
        // BLUE (top right)
        {{13, 0}, {14, 0}, {13, 1}, {14, 1}},
        // GREEN (top left)
        {{0, 0}, {1, 0}, {0, 1}, {1, 1}},
        // YELLOW (bottom left)
        {{0, 13}, {1, 13}, {0, 14}, {1, 14}}
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
            case "RED": return 0;
            case "BLUE": return 1;
            case "GREEN": return 2;
            case "YELLOW": return 3;
            default: throw new IllegalArgumentException("Invalid color: " + color);
        }
    }

    public static Vector3 gridToWorld(int x, int y) {
        float worldX = -7.5f + (x * 1.0f);
        float worldZ = -7.5f + ((14 - y) * 1.0f);
        return new Vector3(worldX, 0, worldZ);
    }
}
