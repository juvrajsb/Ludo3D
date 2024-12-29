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

        // Move red pawn out
        game.movePawn(0, 0, 6);
        game.movePawn(0, 0, 3);

        // Move blue pawn to same position
        blue.getPawns().get(0).setPosition(3);
        game.movePawn(2, 0, 6);

        // Red pawn should be sent home
        assertTrue(red.getPawns().get(0).isHome());
    }

    @Test
    public void testWinningCondition() {
        Player player = players.get(0);
        for (Pawn pawn : player.getPawns()) {
            pawn.setPosition(52); // Move all pawns to home
        }
        assertTrue(game.isGameOver());
        assertEquals(player, game.getWinner());
    }
}
