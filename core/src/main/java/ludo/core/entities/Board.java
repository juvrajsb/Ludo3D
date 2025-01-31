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
        playerStartPositions = new HashMap<>();
        initializePlayerStartPositions();
        safeSpots = new HashMap<>();
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
//public boolean isHomeColumn(int position, String color) {
//    int startPosition = getStartPosition(color);
//
//    // Check if position is on regular board spaces
//    if (position < BOARD_SIZE) {
//        // Logic for regular board positions
//        int homeStart = (startPosition + BOARD_SIZE - HOME_COLUMN_SIZE) % BOARD_SIZE;
//        int homeEnd = (startPosition + BOARD_SIZE - 1) % BOARD_SIZE;
//
//        if (homeStart > homeEnd) {
//            return position >= homeStart || position <= homeEnd;
//        }
//        return position >= homeStart && position <= homeEnd;
//    }
//
//    // Check home column positions (>=52)
//    int homeColumnBaseIndex = BOARD_SIZE + (startPosition / 13) * HOME_COLUMN_SIZE;
//    return position >= homeColumnBaseIndex && position < homeColumnBaseIndex + HOME_COLUMN_SIZE;
//}
//public boolean isHomeColumn(int position, String color) {
//    // For positions in home column area (≥52)
//    if (position >= BOARD_SIZE) {
//        int playerIndex = getStartPosition(color) / 13;
//        int homeStart = BOARD_SIZE + playerIndex * HOME_COLUMN_SIZE;
//        int homeEnd = homeStart + HOME_COLUMN_SIZE - 1;
//        return position >= homeStart && position <= homeEnd;
//    }
//
//    // For positions on main board (<52), never consider them as home column
//    return false;
//}
