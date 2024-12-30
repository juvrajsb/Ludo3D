package ludo.test;

import ludo.Pawn;
import ludo.server.LudoGame;
import ludo.server.Player;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class GameStateTest {
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
    public void testGameStateSerialization() {
        String state = game.getSerializedGameState();
        assertNotNull(state);
        assertTrue(state.contains("RED"));
        assertTrue(state.contains("GREEN"));
        assertTrue(state.contains("BLUE"));
        assertTrue(state.contains("YELLOW"));
    }

    @Test
    public void testPawnCapture() {
        Player red = players.get(0);
        Player blue = players.get(2);

        // Move red pawn out and to position 3
        assertTrue(game.movePawn(0, 0, 6));  // Move out of home
        assertTrue(game.movePawn(0, 0, 3));  // Move to position 3
        System.out.println("Red pawn position: " + red.getPawns().get(0).getPosition());

        // Move blue pawn to capture red (need multiple moves to get to same position)
        assertTrue(game.movePawn(2, 0, 6));  // Move out of home
        assertTrue(game.movePawn(2, 0, 6));  // Move 6 spaces
        assertTrue(game.movePawn(2, 0, 6));  // Move 6 more spaces
        assertTrue(game.movePawn(2, 0, 6));  // Move 6 more spaces
        assertTrue(game.movePawn(2, 0, 6));  // Move 6 more spaces
        assertTrue(game.movePawn(2, 0, 5));  // Final move to reach same position

        // Print final positions to verify
        System.out.println("Red pawn position: " + red.getPawns().get(0).getPosition());
        System.out.println("Blue pawn position: " + blue.getPawns().get(0).getPosition());

        // Red pawn should be sent home
        assertTrue(red.getPawns().get(0).isHome());
    }

    @Test
    public void testWinningCondition() {
        Player player = players.get(0);
        for (Pawn pawn : player.getPawns()) {
            pawn.setPosition(52); // Move all pawns to home
            pawn.setFinished(true);

        }
        assertTrue(game.isGameOver());
        assertEquals(player, game.getWinner());
    }
}
