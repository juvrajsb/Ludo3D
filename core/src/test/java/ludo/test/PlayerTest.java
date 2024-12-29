package ludo.test;

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
        assertEquals(0, player.getPawnsInHome());
        player.getPawns().get(0).setPosition(51); // Set near home column
        assertEquals(0, player.getPawnsInHome()); // Not in home yet
        player.getPawns().get(0).setPosition(52); // Set in home
        assertEquals(1, player.getPawnsInHome());
    }
}



