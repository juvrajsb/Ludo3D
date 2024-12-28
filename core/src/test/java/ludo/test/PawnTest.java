package ludo.test;

import ludo.Pawn;
import ludo.server.*;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

public class PawnTest {
    private Pawn pawn;
    private Board board;

    @Before
    public void setUp() {
        pawn = new Pawn("RED");
        board = new Board();
    }

    @Test
    public void testInitialState() {
        assertTrue(pawn.isHome());
        assertFalse(pawn.isFinished());
        assertEquals(-1, pawn.getPosition());
        assertEquals("RED", pawn.getColor());
    }

    @Test
    public void testMovement() {
        pawn.leaveHome(board);
        assertFalse(pawn.isHome());
        assertEquals(board.getStartPosition("RED"), pawn.getPosition());

        pawn.move(3, board);
        assertEquals(3, pawn.getPosition());

        pawn.sendHome();
        assertTrue(pawn.isHome());
        assertEquals(-1, pawn.getPosition());
    }
}
