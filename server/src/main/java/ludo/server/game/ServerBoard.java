package ludo.server.game;

import ludo.core.utils.Constants;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ServerBoard {
    private final Map<String, Integer> startPositions;
    private final boolean[] safeSpots;
    private final Map<String, Integer[]> homeColumns;

    public ServerBoard() {
        startPositions = new HashMap<>();
        startPositions.put("RED", 0);
        startPositions.put("GREEN", 13);
        startPositions.put("BLUE", 26);
        startPositions.put("YELLOW", 39);

        // Initialize safe spots
        safeSpots = new boolean[Constants.BOARD_SIZE];
        Arrays.fill(safeSpots, false);
        for (int pos : startPositions.values()) {
            safeSpots[pos] = true;
        }

        // Initialize home columns
        homeColumns = new HashMap<>();
        homeColumns.put("RED", new Integer[]{46, 47, 48, 49, 50, 51});
        homeColumns.put("GREEN", new Integer[]{52, 53, 54, 55, 56, 57});
        homeColumns.put("BLUE", new Integer[]{58, 59, 60, 61, 62, 63});
        homeColumns.put("YELLOW", new Integer[]{64, 65, 66, 67, 68, 69});
    }

    public int getStartPosition(String color) {
        return startPositions.getOrDefault(color, -1);
    }

    public boolean isSafeSpot(int position) {
        return position >= 0 && position < Constants.BOARD_SIZE && safeSpots[position];
    }

    public boolean isHomeColumn(int position, String color) {
        Integer[] homeColumn = homeColumns.get(color);
        if (homeColumn == null) return false;
        return Arrays.asList(homeColumn).contains(position);
    }

    public int getTotalSpaces() {
        return Constants.BOARD_SIZE + (Constants.HOME_COLUMN_SIZE * 4);
    }
}
