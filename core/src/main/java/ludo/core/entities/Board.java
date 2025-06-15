package ludo.core.entities;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Board implements Serializable {
    private static final int BOARD_SIZE = 52;
    private static final int HOME_COLUMN_SIZE = 5;

    private Map<String, Integer> playerStartPositions;
    private Map<Integer, Boolean> safeSpots;

    public Board() {
        safeSpots = new HashMap<>(); // Initialize safeSpots map
        playerStartPositions = new HashMap<>();
        initializePlayerStartPositions();
        initializeSafeSpots();
    }

    private void initializeSafeSpots() {
        safeSpots = new HashMap<>();

        // Start positions are safe spots
        safeSpots.put(0, true);   // Yellow start
        safeSpots.put(13, true);  // Blue start
        safeSpots.put(26, true);  // Red start
        safeSpots.put(39, true);  // Green start

        // Additional safe spots
        safeSpots.put(8, true);   // Safe spot near Yellow
        safeSpots.put(21, true);  // Safe spot near Blue
        safeSpots.put(34, true);  // Safe spot near Red
        safeSpots.put(47, true);  // Safe spot near Green
    }

    public boolean isSafeSpot(int position) {
        boolean isSafe = safeSpots.getOrDefault(position, false);
        return isSafe;
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

//    public int getHomeBase(String color) {
//        switch (color.toUpperCase()) {
//            case "YELLOW": return BOARD_SIZE + 3 * HOME_COLUMN_SIZE;
//            case "BLUE": return BOARD_SIZE + 2 * HOME_COLUMN_SIZE;
//            case "RED": return BOARD_SIZE;
//            case "GREEN": return BOARD_SIZE + HOME_COLUMN_SIZE;
//            default: return -1;
//        }
//    }

    public int getStartPosition(String color) {
        return playerStartPositions.getOrDefault(color.toUpperCase(), -1);
    }

//    public int getEntryPoint(String color) {
//        int startPosition = getStartPosition(color);
//        if (startPosition == -1) return -1;
//        int colorIndex;
//        switch (color.toUpperCase()) { //todo check if the color order is correct
//            case "YELLOW": colorIndex = 0; break;
//            case "BLUE": colorIndex = 1; break;
//            case "RED": colorIndex = 2; break;
//            case "GREEN": colorIndex = 3; break;
//            default: return 0;
//        }
//        return BOARD_SIZE + colorIndex * HOME_COLUMN_SIZE;
//    }

//    public int getSafeSpot(String color) {
//        switch (color.toUpperCase()) {
//            case "YELLOW": return 45;
//            case "BLUE": return 32;
//            case "RED": return 6;
//            case "GREEN": return 19;
//            default: return -1;
//        }
//    }

//    public boolean isValidPosition(int position) {
//        if (position < 0) return false;
//        if (position < BOARD_SIZE) return true;
//        if (position < BOARD_SIZE + (HOME_COLUMN_SIZE * 4)) return true;
//        return false;
//    }

    public int getStartPositionIndex(String color) {
        return playerStartPositions.getOrDefault(color.toUpperCase(), -1);
    }
}
