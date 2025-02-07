package ludo.core.utils;

import ludo.core.entities.Cell;

public class Constants {
    // Board dimensions
    public static final int BOARD_SIZE = 52;

    public static final int HOME_COLUMN_SIZE = 6;

    // Player colors
    public static final String RED = "RED";
    public static final String GREEN = "GREEN";
    public static final String BLUE = "BLUE";
    public static final String YELLOW = "YELLOW";

    // Screen dimensions
    public static final int VIEWPORT_WIDTH = 800;
    public static final int VIEWPORT_HEIGHT = 600;

    // UI constants
    public static final float BUTTON_PADDING = 10f;

    // Game constants
    public static final int MAX_PLAYERS = 4;
    public static final int MIN_PLAYERS = 2;
    public static final int PAWNS_PER_PLAYER = 4;
    public static final int DICE_MAX = 6;

    // Home Bases (4x4 cells for each home base)
    public static final Cell[][] HOME_BASES = {
        { new Cell(0, 6, "YELLOW", "home"),
          new Cell(0, 7, "YELLOW", "home"),
          new Cell(0, 8, "YELLOW", "home"),
          new Cell(0, 9, "YELLOW", "home") },
        { new Cell(0, 9, "BLUE", "home"),
          new Cell(0, 10, "BLUE", "home"),
          new Cell(0, 11, "BLUE", "home"),
          new Cell(0, 12, "BLUE", "home") },
        { new Cell(9, 9, "RED", "home"),
          new Cell(9, 10, "RED", "home"),
          new Cell(9, 11, "RED", "home"),
          new Cell(9, 12, "RED", "home") },
        { new Cell(9, 0, "GREEN", "home"),
          new Cell(9, 1, "GREEN", "home"),
          new Cell(9, 2, "GREEN", "home"),
          new Cell(9, 3, "GREEN", "home") }
    };

    // Start Positions
    public static final Cell[] START_POSITIONS = {
        new Cell(6, 1, "YELLOW", "start"),
        new Cell(1, 8, "BLUE", "start"),
        new Cell(8, 13, "RED", "start"),
        new Cell(13, 6, "GREEN", "start")
    };

    // Safe Spots
    public static final Cell[] SAFE_SPOTS = {
        new Cell(1, 6, null, "safe"), // Close to YELLOW
        new Cell(6, 12, null, "safe"), // Close to BLUE
        new Cell(12, 8, null, "safe"), // Close to RED
        new Cell(8, 2, null, "safe") // Close to GREEN
    };

    // Target Bases
    public static final Cell[] TARGET_BASES = {
        new Cell(7, 6, "YELLOW", "target"),
        new Cell(6, 7, "BLUE", "target"),
        new Cell(7, 8, "RED", "target"),
        new Cell(8, 7, "GREEN", "target")
    };

    // Home Columns (5 cells each)
    public static final Cell[][] HOME_COLUMNS = {
        { new Cell(7, 1, "YELLOW", "homeColumn"),
          new Cell(7, 2, "YELLOW", "homeColumn"),
          new Cell(7, 3, "YELLOW", "homeColumn"),
          new Cell(7, 4, "YELLOW", "homeColumn"),
          new Cell(7, 5, "YELLOW", "homeColumn") },
        { new Cell(1, 7, "BLUE", "homeColumn"),
          new Cell(2, 7, "BLUE", "homeColumn"),
          new Cell(3, 7, "BLUE", "homeColumn"),
          new Cell(4, 7, "BLUE", "homeColumn"),
          new Cell(5, 7, "BLUE", "homeColumn") },
        { new Cell(7, 9, "RED", "homeColumn"),
          new Cell(7, 10, "RED", "homeColumn"),
          new Cell(7, 11, "RED", "homeColumn"),
          new Cell(7, 12, "RED", "homeColumn"),
          new Cell(7, 13, "RED", "homeColumn") },
        { new Cell(9, 7, "GREEN", "homeColumn"),
          new Cell(10, 7, "GREEN", "homeColumn"),
          new Cell(11, 7, "GREEN", "homeColumn"),
          new Cell(12, 7, "GREEN", "homeColumn"),
          new Cell(13, 7, "GREEN", "homeColumn") }
    };

    // Home Column Entry Points
    public static final Cell[] HOME_COLUMN_ENTRIES = {
        new Cell(7, 0, "YELLOW", "homeEntry"),
        new Cell(0, 7, "BLUE", "homeEntry"),
        new Cell(7, 14, "RED", "homeEntry"),
        new Cell(14, 7, "GREEN", "homeEntry")
    };

    // Start positions (indices)
    public static final int YELLOW_START = 39;
    public static final int BLUE_START = 26;
    public static final int RED_START = 0;
    public static final int GREEN_START = 13;

    // Safe spots (indices)
    public static final int YELLOW_SAFE_SPOT = 45;
    public static final int BLUE_SAFE_SPOT = 32;
    public static final int RED_SAFE_SPOT = 6;
    public static final int GREEN_SAFE_SPOT = 19;

    // Home bases (positions in home column)
    public static final int YELLOW_HOME_BASE = BOARD_SIZE + 3 * HOME_COLUMN_SIZE;
    public static final int BLUE_HOME_BASE = BOARD_SIZE + 2 * HOME_COLUMN_SIZE;
    public static final int RED_HOME_BASE = BOARD_SIZE;
    public static final int GREEN_HOME_BASE = BOARD_SIZE + HOME_COLUMN_SIZE;
}
