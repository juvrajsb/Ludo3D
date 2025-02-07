package ludo.core.entities;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Board implements Serializable {
    private static final int BOARD_SIZE = 52;  // Total spaces on main track
    private static final int HOME_COLUMN_SIZE = 6;
    private static final int GRID_SIZE = 15;   // 15x15 grid

    private Cell[][] cells; // 2D array of cells
    private Map<String, Integer> playerStartPositions;
    private Map<Integer, Boolean> safeSpots;

    public Board() {
        cells = new Cell[GRID_SIZE][GRID_SIZE]; // Initialize the grid
        safeSpots = new HashMap<>(); // Initialize safeSpots map
        playerStartPositions = new HashMap<>();
        initializePlayerStartPositions();
        initializeSafeSpots();
        initializeBoardLayout();
    }

    private void initializeBoardLayout() {
        // Initialize main track
        initializeMainTrack();
        // Initialize home bases
        initializeHomeBases();
        // Initialize start positions
        initializeStartPositions();
        // Initialize home columns
        initializeHomeColumns();
        // Initialize safe spots
        initializeSafeSpots();
    }

    private void initializeMainTrack() {
        // Initialize using the same coordinates as BoardCoordinates
        // Bottom edge (RED path)
        for (int i = 0; i < 13; i++) {
            mapPosition(i, new Cell(7, 14 - i, "RED", "normal"));
        }

        // Right edge (BLUE path)
        for (int i = 13; i < 26; i++) {
            mapPosition(i, new Cell(13 - (i - 13), 7, "BLUE", "normal"));
        }

        // Top edge (GREEN path)
        for (int i = 26; i < 39; i++) {
            mapPosition(i, new Cell(6, 1 + (i - 26), "GREEN", "normal"));
        }

        // Left edge (YELLOW path)
        for (int i = 39; i < 52; i++) {
            mapPosition(i, new Cell(i - 39, 8, "YELLOW", "normal"));
        }
    }

    private void initializeHomeColumns() {
        // RED home column (moves up)
        for (int i = 0; i < HOME_COLUMN_SIZE; i++) {
            mapPosition(BOARD_SIZE + i, new Cell(7, 9 + i, "RED", "home"));
        }

        // BLUE home column (moves left)
        for (int i = 0; i < HOME_COLUMN_SIZE; i++) {
            mapPosition(BOARD_SIZE + HOME_COLUMN_SIZE + i, new Cell(12 - i, 7, "BLUE", "home"));
        }

        // GREEN home column (moves right)
        for (int i = 0; i < HOME_COLUMN_SIZE; i++) {
            mapPosition(BOARD_SIZE + (2 * HOME_COLUMN_SIZE) + i, new Cell(1 + i, 7, "GREEN", "home"));
        }

        // YELLOW home column (moves down)
        for (int i = 0; i < HOME_COLUMN_SIZE; i++) {
            mapPosition(BOARD_SIZE + (3 * HOME_COLUMN_SIZE) + i, new Cell(7, 5 - i, "YELLOW", "home"));
        }
    }

    private void initializeHomeBases() {
        // RED home base (bottom right)
        cells[13][13] = new Cell(13, 13, "RED", "base");
        cells[14][13] = new Cell(14, 13, "RED", "base");
        cells[13][14] = new Cell(13, 14, "RED", "base");
        cells[14][14] = new Cell(14, 14, "RED", "base");

        // BLUE home base (top right)
        cells[13][0] = new Cell(13, 0, "BLUE", "base");
        cells[14][0] = new Cell(14, 0, "BLUE", "base");
        cells[13][1] = new Cell(13, 1, "BLUE", "base");
        cells[14][1] = new Cell(14, 1, "BLUE", "base");

        // GREEN home base (top left)
        cells[0][0] = new Cell(0, 0, "GREEN", "base");
        cells[1][0] = new Cell(1, 0, "GREEN", "base");
        cells[0][1] = new Cell(0, 1, "GREEN", "base");
        cells[1][1] = new Cell(1, 1, "GREEN", "base");

        // YELLOW home base (bottom left)
        cells[0][13] = new Cell(0, 13, "YELLOW", "base");
        cells[1][13] = new Cell(1, 13, "YELLOW", "base");
        cells[0][14] = new Cell(0, 14, "YELLOW", "base");
        cells[1][14] = new Cell(1, 14, "YELLOW", "base");
    }

//    private void initializeHomeColumns() {
//        // RED home column (moves up)
//        for (int i = 0; i < HOME_COLUMN_SIZE; i++) {
//            int position = BOARD_SIZE + i;
//            mapPosition(position, new Cell(7, 8 + i, "RED", "home"));
//        }
//
//        // BLUE home column (moves left)
//        for (int i = 0; i < HOME_COLUMN_SIZE; i++) {
//            int position = BOARD_SIZE + HOME_COLUMN_SIZE + i;
//            mapPosition(position, new Cell(8 - i, 7, "BLUE", "home"));
//        }
//
//        // GREEN home column (moves right)
//        for (int i = 0; i < HOME_COLUMN_SIZE; i++) {
//            int position = BOARD_SIZE + (2 * HOME_COLUMN_SIZE) + i;
//            mapPosition(position, new Cell(6 + i, 7, "GREEN", "home"));
//        }
//
//        // YELLOW home column (moves down)
//        for (int i = 0; i < HOME_COLUMN_SIZE; i++) {
//            int position = BOARD_SIZE + (3 * HOME_COLUMN_SIZE) + i;
//            mapPosition(position, new Cell(7, 6 - i, "YELLOW", "home"));
//        }
//    }
//
//    private void initializeHomeBases() {
//        // RED home base (bottom right)
//        cells[13][13] = new Cell(13, 13, "RED", "base");
//        cells[13][14] = new Cell(13, 14, "RED", "base");
//        cells[14][13] = new Cell(14, 13, "RED", "base");
//        cells[14][14] = new Cell(14, 14, "RED", "base");
//
//        // BLUE home base (bottom left)
//        cells[13][0] = new Cell(13, 0, "BLUE", "base");
//        cells[13][1] = new Cell(13, 1, "BLUE", "base");
//        cells[14][0] = new Cell(14, 0, "BLUE", "base");
//        cells[14][1] = new Cell(14, 1, "BLUE", "base");
//
//        // GREEN home base (top left)
//        cells[0][0] = new Cell(0, 0, "GREEN", "base");
//        cells[0][1] = new Cell(0, 1, "GREEN", "base");
//        cells[1][0] = new Cell(1, 0, "GREEN", "base");
//        cells[1][1] = new Cell(1, 1, "GREEN", "base");
//
//        // YELLOW home base (top right)
//        cells[0][13] = new Cell(0, 13, "YELLOW", "base");
//        cells[0][14] = new Cell(0, 14, "YELLOW", "base");
//        cells[1][13] = new Cell(1, 13, "YELLOW", "base");
//        cells[1][14] = new Cell(1, 14, "YELLOW", "base");
//    }

    private void initializeStartPositions() {
        // Set start positions with proper cells
        cells[8][13] = new Cell(8, 13, "RED", "start");
        cells[13][6] = new Cell(13, 6, "GREEN", "start");
        cells[1][8] = new Cell(1, 8, "BLUE", "start");
        cells[6][1] = new Cell(6, 1, "YELLOW", "start");
    }

    private void initializeSafeSpots() {
        // Initialize map before using it
        safeSpots = new HashMap<>();

        // Start positions are safe spots
        safeSpots.put(0, true);   // Red start
        safeSpots.put(13, true);  // Green start
        safeSpots.put(26, true);  // Blue start
        safeSpots.put(39, true);  // Yellow start

        // Additional safe spots
        safeSpots.put(8, true);   // Safe spot
        safeSpots.put(21, true);  // Safe spot
        safeSpots.put(34, true);  // Safe spot
        safeSpots.put(47, true);  // Safe spot

        // Set safe spots with proper cells
        cells[12][8] = new Cell(12, 8, null, "safe");  // Red safe spot
        cells[8][2] = new Cell(8, 2, null, "safe");    // Green safe spot
        cells[6][12] = new Cell(6, 12, null, "safe");  // Blue safe spot
        cells[1][6] = new Cell(1, 6, null, "safe");    // Yellow safe spot
    }

    private void mapPosition(int boardPosition, Cell cell) {
        if (boardPosition >= 0 && cell != null) {
            int x = cell.getX();
            int y = cell.getY();
            cells[x][y] = cell; // Place the cell in the grid
        }
    }

//    private void initializeHomeColumns() {
//        // RED home column
//        for (int i = 0; i < HOME_COLUMN_SIZE; i++) {
//            int position = BOARD_SIZE + i;
//            mapPosition(position, new Cell(7, 8 + i, "RED", "home"));
//        }
//        // GREEN home column
//        for (int i = 0; i < HOME_COLUMN_SIZE; i++) {
//            int position = BOARD_SIZE + HOME_COLUMN_SIZE + i;
//            mapPosition(position, new Cell(8 + i, 7, "GREEN", "home"));
//        }
//        // BLUE home column
//        for (int i = 0; i < HOME_COLUMN_SIZE; i++) {
//            int position = BOARD_SIZE + (2 * HOME_COLUMN_SIZE) + i;
//            mapPosition(position, new Cell(2 + i, 7, "BLUE", "home"));
//        }
//        // YELLOW home column
//        for (int i = 0; i < HOME_COLUMN_SIZE; i++) {
//            int position = BOARD_SIZE + (3 * HOME_COLUMN_SIZE) + i;
//            mapPosition(position, new Cell(7, 2 + i, "YELLOW", "home"));
//        }
//    }

    public Cell getCell(int x, int y) {
        return cells[x][y];
    }

//    public int getStartPosition(String color) {
//        return playerStartPositions.get(color.toUpperCase());
//    }

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
            int colorIndex;
            switch (color.toUpperCase()) {
                case "RED": colorIndex = 0; break;
                case "GREEN": colorIndex = 1; break;
                case "BLUE": colorIndex = 2; break;
                case "YELLOW": colorIndex = 3; break;
                default: return false;
            }
            int homeStart = BOARD_SIZE + colorIndex * HOME_COLUMN_SIZE;
            int homeEnd = homeStart + HOME_COLUMN_SIZE - 1;
            return position >= homeStart && position <= homeEnd;
        }
        return false;
    }

    private void initializePlayerStartPositions() {
        playerStartPositions = new HashMap<>();
        playerStartPositions.put("RED", 0);
        playerStartPositions.put("GREEN", 13);
        playerStartPositions.put("BLUE", 26);
        playerStartPositions.put("YELLOW", 39);
    }

    public Cell getGridPosition(int boardPosition) {
        int x = boardPosition % GRID_SIZE;
        int y = boardPosition / GRID_SIZE;
        return getCell(x, y);
    }

    public int getHomeBase(String color) {
        switch (color.toUpperCase()) {
            case "YELLOW": return BOARD_SIZE + 3 * HOME_COLUMN_SIZE;
            case "BLUE": return BOARD_SIZE + 2 * HOME_COLUMN_SIZE;
            case "RED": return BOARD_SIZE;
            case "GREEN": return BOARD_SIZE + HOME_COLUMN_SIZE;
            default: return -1;
        }
    }

    public int getStartPosition(String color) {
        return playerStartPositions.getOrDefault(color.toUpperCase(), -1);
    }

    public int getSafeSpot(String color) {
        switch (color.toUpperCase()) {
            case "YELLOW": return 45;
            case "BLUE": return 32;
            case "RED": return 6;
            case "GREEN": return 19;
            default: return -1;
        }
    }

    // Add new helper method for position validation
    public boolean isValidPosition(int position) {
        if (position < 0) return false;
        if (position < BOARD_SIZE) return true;
        if (position < BOARD_SIZE + (HOME_COLUMN_SIZE * 4)) return true;
        return false;
    }

    // Add new method to get cell position index
    public int getCellPositionIndex(Cell cell) {
        if (cell == null) return -1;

        // Check main board positions
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (getGridPosition(i).equals(cell)) {
                return i;
            }
        }

        // Check home columns
        for (int i = 0; i < 4; i++) {  // For each player color
            for (int j = 0; j < HOME_COLUMN_SIZE; j++) {
                int position = BOARD_SIZE + (i * HOME_COLUMN_SIZE) + j;
                if (getGridPosition(position).equals(cell)) {
                    return position;
                }
            }
        }

        return -1;  // Position not found
    }

    // Update getStartPosition to return position index when needed
    public int getStartPositionIndex(String color) {
        return playerStartPositions.getOrDefault(color.toUpperCase(), -1);
    }

    // Fix the Cell type mismatch by converting position index to Cell
    public Cell getStartCell(String color) {
        int startPos = getStartPosition(color);
        return getGridPosition(startPos);
    }

    public Cell getStartPositionCell(String color) {
        int position = getStartPosition(color);
        return getGridPosition(position);
    }


//        public ludo.core.model.Cell getHomeBase(String color) {
//            int colorIndex;
//            switch (color.toUpperCase()) {
//                case "YELLOW": colorIndex = 0; break;
//                case "BLUE": colorIndex = 1; break;
//                case "RED": colorIndex = 2; break;
//                case "GREEN": colorIndex = 3; break;
//                default: return null;
//            }
//            return new ludo.core.model.Cell(Constants.HOME_BASES[colorIndex][0], Constants.HOME_BASES[colorIndex][1]);
//        }
//
//        public ludo.core.model.Cell getStartPosition(String color) {
//            int colorIndex;
//            switch (color.toUpperCase()) {
//                case "YELLOW": colorIndex = 0; break;
//                case "BLUE": colorIndex = 1; break;
//                case "RED": colorIndex = 2; break;
//                case "GREEN": colorIndex = 3; break;
//                default: return null;
//            }
//            return new ludo.core.model.Cell(Constants.START_POSITIONS[colorIndex][0], Constants.START_POSITIONS[colorIndex][1]);
//        }
//
//        public ludo.core.model.Cell getSafeSpot(String color) {
//            int colorIndex;
//            switch (color.toUpperCase()) {
//                case "YELLOW": colorIndex = 0; break;
//                case "BLUE": colorIndex = 1; break;
//                case "RED": colorIndex = 2; break;
//                case "GREEN": colorIndex = 3; break;
//                default: return null;
//            }
//            return new ludo.core.model.Cell(Constants.SAFE_SPOTS[colorIndex][0], Constants.SAFE_SPOTS[colorIndex][1]);
//        }
//        // ...existing code...
//    }
}
