//package ludo.core.utils;
//
//public class BoardCoordinatesMapping {
//    // This is a basic example: assume your main track uses sequential positions mapped onto desired grid coordinates.
//    // You can redesign this mapping to suit your game.
//    // For instance, on a 15x15 grid:
//    private static final int[][] MAIN_TRACK_MAPPING = new int[][] {
//        // boardPosition 0 maps to grid row, col; position 1, position 2, etc.
//        {7, 13}, {7, 12}, {7, 11}, {7, 10}, {7, 9}, {7, 8},
//        {8, 7}, {9, 7}, {10, 7}, {11, 7}, {12, 7}, {13, 7},
//        {13, 6}, {13, 5}, {13, 4}, {13, 3}, {13, 2}, {13, 1},
//        {12, 0}, {11, 0}, {10, 0}, {9, 0}, {8, 0}, {7, 0},
//        {7, 1}, {7, 2}, {7, 3}, {7, 4}, {7, 5}, {7, 6},
//        {6, 7}, {5, 7}, {4, 7}, {3, 7}, {2, 7}, {1, 7},
//        {0, 7}, {0, 8}, {0, 9}, {0, 10}, {0, 11}, {0, 12},
//        {0, 13}, {1, 13}, {2, 13}, {3, 13}, {4, 13}, {5, 13}, {6, 13}
//    };
//
//    public static int[] getGridCoordsForPosition(int boardPosition) {
//        if (boardPosition < 0 || boardPosition >= MAIN_TRACK_MAPPING.length) {
//            return null;
//        }
//        return MAIN_TRACK_MAPPING[boardPosition];
//    }
//}
