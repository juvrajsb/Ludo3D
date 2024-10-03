package ludo.server;

import java.util.HashMap;
import java.util.Map;

public class Board {
    private static final int BOARD_SIZE = 52;
    private static final int HOME_COLUMN_SIZE = 6;
    private Map<String, Integer> playerStartPositions;
    private Map<Integer, Boolean> safeSpots;

    public Board() {
        initializePlayerStartPositions();
        initializeSafeSpots();
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
        safeSpots.put(0, true);
        safeSpots.put(8, true);
        safeSpots.put(13, true);
        safeSpots.put(21, true);
        safeSpots.put(26, true);
        safeSpots.put(34, true);
        safeSpots.put(39, true);
        safeSpots.put(47, true);
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
        int startPosition = getStartPosition(color);
        return position >= startPosition + BOARD_SIZE - HOME_COLUMN_SIZE && position < startPosition + BOARD_SIZE;
    }
}
