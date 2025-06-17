package ludo.core.entities;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Board implements Serializable {
    private static final int BOARD_SIZE = 52;
    private static final int HOME_COLUMN_SIZE = 6;

    private Map<String, Integer> playerStartPositions;
    private Map<Integer, Boolean> safeSpots;

    public Board() {
        safeSpots = new HashMap<>();
        playerStartPositions = new HashMap<>();
        initializePlayerStartPositions();
        initializeSafeSpots();
    }

    private void initializeSafeSpots() {
        safeSpots = new HashMap<>();
        safeSpots.put(0, true);   // Yellow start
        safeSpots.put(13, true);  // Blue start
        safeSpots.put(26, true);  // Red start
        safeSpots.put(39, true);  // Green start
        safeSpots.put(8, true);   // Safe spot near Yellow
        safeSpots.put(21, true);  // Safe spot near Blue
        safeSpots.put(34, true);  // Safe spot near Red
        safeSpots.put(47, true);  // Safe spot near Green
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
        if (position < BOARD_SIZE) {
            return false;
        }
        int homeStart;
        switch (color.toUpperCase()) {
            case "YELLOW":
                homeStart = 52;
                break;
            case "BLUE":
                homeStart = 58;
                break;
            case "RED":
                homeStart = 64;
                break;
            case "GREEN":
                homeStart = 70;
                break;
            default:
                return false;
        }
        int homeEnd = homeStart + HOME_COLUMN_SIZE -1;
        return position >= homeStart && position <= homeEnd;
    }

    public boolean isTargetBase(int position, String color) {
        switch (color.toUpperCase()) {
            case "YELLOW":
                return position == 57;
            case "BLUE":
                return position == 63;
            case "RED":
                return position == 69;
            case "GREEN":
                return position == 75;
            default:
                return false;
        }
    }

    private void initializePlayerStartPositions() {
        playerStartPositions = new HashMap<>();
        playerStartPositions.put("YELLOW", 0);
        playerStartPositions.put("BLUE", 13);
        playerStartPositions.put("RED", 26);
        playerStartPositions.put("GREEN", 39);
    }

    public int getStartPosition(String color) {
        return playerStartPositions.getOrDefault(color.toUpperCase(), -1);
    }

    public int getStartPositionIndex(String color) {
        return playerStartPositions.getOrDefault(color.toUpperCase(), -1);
    }
}
