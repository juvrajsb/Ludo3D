package ludo.test;

import ludo.core.entities.Board;
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
        assertTrue(board.isSafeSpot(0));   // Red start
        assertTrue(board.isSafeSpot(13));  // Green start
        assertTrue(board.isSafeSpot(26));  // Blue start
        assertTrue(board.isSafeSpot(39));  // Yellow start
        assertTrue(board.isSafeSpot(8));   // Safe spot
        assertTrue(board.isSafeSpot(21));  // Safe spot
        assertTrue(board.isSafeSpot(34));  // Safe spot
        assertTrue(board.isSafeSpot(47));  // Safe spot
        assertFalse(board.isSafeSpot(1));  // Regular spot
    }

    @Test
    public void testHomeColumn() {
        assertTrue(board.isHomeColumn(52, "RED"));    // First home column position
        assertTrue(board.isHomeColumn(57, "RED"));    // Last home column position
        assertTrue(board.isHomeColumn(58, "GREEN"));  // First green home position
        assertFalse(board.isHomeColumn(20, "RED"));  // Regular board position
    }
}
