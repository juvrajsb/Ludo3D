package ludo.test;

import ludo.server.*;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;


public class BoardTest {
    private Board board;

    @Before
    public void setUp() {
        board = new Board();
    }

    @Test
    public void testStartPositions() {
        assertEquals(0, board.getStartPosition("RED"));
        assertEquals(13, board.getStartPosition("GREEN"));
        assertEquals(26, board.getStartPosition("BLUE"));
        assertEquals(39, board.getStartPosition("YELLOW"));
    }

    @Test
    public void testSafeSpots() {
        assertTrue(board.isSafeSpot(0));  // Red start
        assertTrue(board.isSafeSpot(13)); // Green start
        assertTrue(board.isSafeSpot(26)); // Blue start
        assertTrue(board.isSafeSpot(39)); // Yellow start
        assertFalse(board.isSafeSpot(1)); // Regular spot
    }

    @Test
    public void testHomeColumn() {
        assertTrue(board.isHomeColumn(47, "RED"));    // Red home column
        assertTrue(board.isHomeColumn(60, "GREEN"));   // Green home column
        assertFalse(board.isHomeColumn(20, "RED"));   // Not in home column
    }
}


