package ludo.test;

import ludo.core.entities.*;
import ludo.core.game.LudoGame;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class LudoGameTest {
    private LudoGame game;
    private List<Player> players;

    @Before
    public void setUp() {
        game = new LudoGame();
        players = new ArrayList<>();
        players.add(new Player("Player1", "RED"));
        players.add(new Player("Player2", "GREEN"));
        players.add(new Player("Player3", "BLUE"));
        players.add(new Player("Player4", "YELLOW"));
        game.initializeGame(players);
    }

    @Test
    public void testInitialGameState() {
        assertEquals(4, game.getPlayers().size());
        for (Player player : game.getPlayers()) {
            assertEquals(4, player.getPawns().size());
            for (Pawn pawn : player.getPawns()) {
                assertTrue(pawn.isHome());
                assertEquals(-1, pawn.getPosition());
            }
        }
    }

    @Test
    public void testDiceRoll() {
        for (int i = 0; i < 100; i++) {
            int roll = game.rollDice();
            assertTrue(roll >= 1 && roll <= 6);
        }
    }

    @Test
    public void testMovePawn() {
        Player player = game.getPlayers().get(0);
        int startPos = game.getBoard().getStartPosition(player.getColor());

        // Test moving pawn out of home
        assertTrue(game.movePawn(0, 0, 6)); // First player, first pawn, roll of 6
        assertEquals(startPos, player.getPawns().get(0).getPosition());
        assertFalse(player.getPawns().get(0).isHome());

        // Test regular move
        assertTrue(game.movePawn(0, 0, 3));
        assertEquals(startPos + 3, player.getPawns().get(0).getPosition());
    }

    @Test
    public void testInvalidMoves() {
        // Test moving without a 6 from home
        assertFalse(game.movePawn(0, 0, 3));

        // Test moving with invalid indices
        assertThrows(IllegalArgumentException.class, () -> {
            game.movePawn(-1, 0, 6);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            game.movePawn(0, -1, 6);
        });
    }
}
