package ludo.test;

import ludo.Pawn;
import ludo.server.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PlayerTest {
    private Player player;
    private Board board;

    @Before
    public void setUp() {
        player = new Player("TestPlayer", "RED");
        board = new Board();
    }

    @Test
    public void testPlayerInitialization() {
        assertEquals("TestPlayer", player.getName());
        assertEquals("RED", player.getColor());
        assertEquals(4, player.getPawns().size());
        assertFalse(player.isUseSingleDie());
    }

    @Test
    public void testPawnManagement() {
        Board board = new Board();
        assertEquals(0, player.getPawnsInHome());  // No pawns finished
        assertEquals(0, player.getPawnsInHomeColumn(board)); // No pawns in home column

        Pawn pawn = player.getPawns().get(0);
        pawn.setPosition(45);  // Before home column (RED home column is 46-51)
        assertEquals(0, player.getPawnsInHomeColumn(board)); // Still not in home column

        pawn.setPosition(47);  // In home column
        assertEquals(0, player.getPawnsInHome());  // Not finished
        assertEquals(1, player.getPawnsInHomeColumn(board)); // But in home column!

        pawn.setFinished(true); // Finally reaches home
        assertEquals(1, player.getPawnsInHome());
    }
}



