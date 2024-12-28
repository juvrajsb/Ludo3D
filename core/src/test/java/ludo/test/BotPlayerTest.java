package ludo.test;

import ludo.Pawn;
import ludo.server.*;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

public class BotPlayerTest {
    private BotPlayer bot;
    private Board board;

    @Before
    public void setUp() {
        bot = new BotPlayer("TestBot", "RED");
        board = new Board();
    }

    @Test
    public void testMakeMove() {
        // Test bot move with no movable pawns
        String move = bot.makeMove(board, 3);
        assertEquals("SKIP", move);

        // Test bot move with movable pawn (after getting a 6)
        move = bot.makeMove(board, 6);
        assertTrue(move.startsWith("MOVE"));
    }
}
