package ludo.core.entities;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Board implements Serializable {
    private static final int BOARD_SIZE = 52;  // Total spaces on main track
    private static final int HOME_COLUMN_SIZE = 6;
    private static final int GRID_SIZE = 15;   // 15x15 grid //TODO check usage not used currently

    private Map<String, Integer> playerStartPositions;
    private Map<Integer, Boolean> safeSpots;
    private Map<Integer, GridPosition> positionToGrid;  // Maps board position to grid coordinates
    private Map<GridPosition, Integer> gridToPosition;  // Maps grid coordinates to board position

    public Board() {
        initializePlayerStartPositions();
        initializeSafeSpots();
        initializeBoardLayout();
    }

    private void initializePlayerStartPositions() {
        playerStartPositions = new HashMap<>();
        playerStartPositions.put("RED", 0);
        playerStartPositions.put("GREEN", 13);
        playerStartPositions.put("BLUE", 26);
        playerStartPositions.put("YELLOW", 39);
    }

    private void initializeSafeSpots() {
        safeSpots = new HashMap<>();
        // Start positions
        safeSpots.put(0, true);   // Red start
        safeSpots.put(13, true);  // Green start
        safeSpots.put(26, true);  // Blue start
        safeSpots.put(39, true);  // Yellow start
        // Safe spots before home columns
        safeSpots.put(8, true);   // Before red home
        safeSpots.put(21, true);  // Before green home
        safeSpots.put(34, true);  // Before blue home
        safeSpots.put(47, true);  // Before yellow home
    }

    private void initializeBoardLayout() {
        positionToGrid = new HashMap<>();
        gridToPosition = new HashMap<>();

        initializeHomePositions();

        // RED path (bottom)
        for (int i = 0; i < 6; i++) {
            mapPosition(i, new GridPosition(14, 8 + i));  // Bottom to right
        }
        for (int i = 6; i < 13; i++) {
            mapPosition(i, new GridPosition(14 - (i - 5), 14));  // Right to top
        }

        // GREEN path (right)
        for (int i = 13; i < 19; i++) {
            mapPosition(i, new GridPosition(8 - (i - 13), 14));  // Right to top
        }
        for (int i = 19; i < 26; i++) {
            mapPosition(i, new GridPosition(0, 14 - (i - 18)));  // Top to left
        }

        // BLUE path (top)
        for (int i = 26; i < 32; i++) {
            mapPosition(i, new GridPosition(0, 8 - (i - 26)));  // Top to left
        }
        for (int i = 32; i < 39; i++) {
            mapPosition(i, new GridPosition(i - 31, 0));  // Left to bottom
        }

        // YELLOW path (left)
        for (int i = 39; i < 45; i++) {
            mapPosition(i, new GridPosition(8 + (i - 39), 0));  // Left to bottom
        }
        for (int i = 45; i < 52; i++) {
            mapPosition(i, new GridPosition(14, i - 44));  // Bottom to right
        }

        // Home columns
        initializeHomeColumns();
    }

    private void initializeHomeColumns() {
        // RED home column (vertical up from bottom)
        for (int i = 0; i < HOME_COLUMN_SIZE; i++) {
            mapPosition(BOARD_SIZE + i, new GridPosition(13, 8 + i));
        }

        // GREEN home column (horizontal left from right)
        for (int i = 0; i < HOME_COLUMN_SIZE; i++) {
            mapPosition(BOARD_SIZE + HOME_COLUMN_SIZE + i, new GridPosition(8 + i, 13));
        }

        // BLUE home column (vertical down from top)
        for (int i = 0; i < HOME_COLUMN_SIZE; i++) {
            mapPosition(BOARD_SIZE + 2 * HOME_COLUMN_SIZE + i, new GridPosition(1, 8 - i));
        }

        // YELLOW home column (horizontal right from left)
        for (int i = 0; i < HOME_COLUMN_SIZE; i++) {
            mapPosition(BOARD_SIZE + 3 * HOME_COLUMN_SIZE + i, new GridPosition(8 - i, 1));
        }
    }

    private void mapPosition(int boardPosition, GridPosition gridPosition) {
        if (boardPosition >= 0 && gridPosition != null) {
            positionToGrid.put(boardPosition, gridPosition);
            gridToPosition.put(gridPosition, boardPosition);
        }
    }

    private void initializeHomePositions() {
        // Red home positions
        for (int i = 0; i < 4; i++) {
            mapPosition(-1, new GridPosition(14 - i, 14 - i)); // Red home positions
        }

        // Blue home positions
        for (int i = 0; i < 4; i++) {
            mapPosition(-1, new GridPosition(0 + i, 0 + i)); // Blue home positions
        }

        // Green home positions
        for (int i = 0; i < 4; i++) {
            mapPosition(-1, new GridPosition(14 - i, 0 + i)); // Green home positions
        }

        // Yellow home positions
        for (int i = 0; i < 4; i++) {
            mapPosition(-1, new GridPosition(0 + i, 14 - i)); // Yellow home positions
        }
    }

    public static class GridPosition {
        public final int x;
        public final int y;

        public GridPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GridPosition that = (GridPosition) o;
            return x == that.x && y == that.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    // Getters for the grid mapping
    public GridPosition getGridPosition(int boardPosition) {
        return positionToGrid.get(boardPosition);
    }

    public Integer getBoardPosition(int gridX, int gridY) {//TODO check usage not used currently
        return gridToPosition.get(new GridPosition(gridX, gridY));
    }

    public int getStartPosition(String color) {
        return playerStartPositions.get(color);
    }

    public boolean isSafeSpot(int position) {
        return safeSpots.getOrDefault(position, false);
    }

    public int getTotalSpaces() {
        return BOARD_SIZE;
    }

    public int getHomeColumnSize() {
        return HOME_COLUMN_SIZE;
    }

    public boolean isHomeColumn(int position, String color) {
        // For positions in home column area (≥52)
        if (position >= BOARD_SIZE) {
            int playerIndex = getStartPosition(color) / 13;
            int homeStart = BOARD_SIZE + playerIndex * HOME_COLUMN_SIZE;
            int homeEnd = homeStart + HOME_COLUMN_SIZE - 1;
            return position >= homeStart && position <= homeEnd;
        }

    // For positions on main board (<52), never consider them as home column
    return false;
    }
}
