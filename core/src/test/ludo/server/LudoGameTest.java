package src.test.ludo.server;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class LudoGameTest {
    private LudoGame game;

    @Before
    public void setUp() {
        game = new LudoGame();
        game.initializeGame();
    }

    @Test
    public void testInitializeGame() {
        assertEquals(4, game.getPlayers().size());
        for (Player player : game.getPlayers()) {
            for (int i = 0; i < 4; i++) {
                assertEquals(-1, player.getPawnPosition(i));
            }
        }
    }

    @Test
    public void testRollDice() {
        int[] roll = game.rollDice();
        assertEquals(2, roll.length);
        assertTrue(roll[0] >= 1 && roll[0] <= 6);
        assertTrue(roll[1] >= 1 && roll[1] <= 6);
    }

    @Test
    public void testMovePawn_ValidMove() {
        assertTrue(game.movePawn(0, 0, 6)); // Move first pawn of first player out of start
        assertEquals(0, game.getPlayers().get(0).getPawnPosition(0));
    }

    @Test
    public void testMovePawn_InvalidMove() {
        assertFalse(game.movePawn(0, 0, 5)); // Can't move out of start without a 6
    }

    @Test
    public void testMovePawn_Capture() {
        game.movePawn(0, 0, 6); // Move first player's pawn out
        game.movePawn(0, 0, 10); // Move to position 10
        game.movePawn(1, 0, 6); // Move second player's pawn out
        game.movePawn(1, 0, 4); // Move to position 10, capturing first player's pawn
        assertEquals(10, game.getPlayers().get(1).getPawnPosition(0));
        assertEquals(-1, game.getPlayers().get(0).getPawnPosition(0));
    }

    @Test
    public void testIsGameOver() {
        assertFalse(game.isGameOver());
        // Move all pawns of first player to home
        for (int i = 0; i < 4; i++) {
            game.getPlayers().get(0).setPawnPosition(i, 58); // Assuming 58 is the last home position
        }
        assertTrue(game.isGameOver());
    }

    @Test
    public void testGetWinner() {
        assertNull(game.getWinner());
        // Move all pawns of first player to home
        for (int i = 0; i < 4; i++) {
            game.getPlayers().get(0).setPawnPosition(i, 58); // Assuming 58 is the last home position
        }
        assertEquals(game.getPlayers().get(0), game.getWinner());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMovePawn_InvalidPlayerIndex() {
        game.movePawn(4, 0, 6);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMovePawn_InvalidPawnIndex() {
        game.movePawn(0, 4, 6);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMovePawn_InvalidSteps() {
        game.movePawn(0, 0, 13);
    }
}