package ludo.test;

import ludo.core.entities.*;
import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class BotPlayerTest {
    private BotPlayer bot;
    private Board board;
    private List<Player> players;

    @Before
    public void setUp() {
        // Initialize bot and board
        bot = new BotPlayer("TestBot", "RED");
        board = new Board();

        // Set up players list for testing
        players = new ArrayList<>();
        players.add(bot);
        players.add(new Player("Player2", "BLUE"));
        players.add(new Player("Player3", "GREEN"));
        players.add(new Player("Player4", "YELLOW"));
    }

    @Test
    public void testNoValidMoves() {
        // Test bot move with no movable pawns (roll of 3)
        int moveIndex = bot.chooseBestMove(board, 3, players);
        assertEquals("Should return -1 when no valid moves", -1, moveIndex);
    }

    @Test
    public void testMoveFromHome() {
        // Test bot move with a 6 (can move out of home)
        int moveIndex = bot.chooseBestMove(board, 6, players);
        assertTrue("Should return valid pawn index for move from home", moveIndex >= 0 && moveIndex < 4);
    }

    @Test
    public void testCapturePreference() {
        // Set up a capture opportunity
        Pawn botPawn = bot.getPawns().get(0);
        botPawn.leaveHome(board); // Move pawn out of home

        Player opponent = players.get(1);
        Pawn opponentPawn = opponent.getPawns().get(0);
        opponentPawn.leaveHome(board);

        // Position opponent pawn where bot can capture it
        int capturePosition = (board.getStartPosition(bot.getColor()) + 6) % board.getTotalSpaces();
        opponentPawn.setPosition(capturePosition);

        // Bot should prefer the move that captures
        botPawn.setPosition(capturePosition - 3);
        int moveIndex = bot.chooseBestMove(board, 3, players);

        assertEquals("Should choose pawn that can capture", 0, moveIndex);
    }

    @Test
    public void testSafeSpotPreference() {
        // Set up a pawn that can move to a safe spot
        Pawn botPawn = bot.getPawns().get(0);
        botPawn.leaveHome(board);

        // Find nearest safe spot
        int safeSpot = findNearestSafeSpot(board, botPawn.getPosition());
        int distance = (safeSpot - botPawn.getPosition() + board.getTotalSpaces()) % board.getTotalSpaces();

        botPawn.setPosition(safeSpot - distance);

        int moveIndex = bot.chooseBestMove(board, distance, players);
        assertEquals("Should choose pawn that can reach safe spot", 0, moveIndex);
    }

    @Test
    public void testHomeStretchPreference() {
        // Set up a pawn near home stretch
        Pawn botPawn = bot.getPawns().get(0);
        int homeEntry = (board.getStartPosition(bot.getColor()) + board.getTotalSpaces() - 1) % board.getTotalSpaces();
        botPawn.setPosition(homeEntry - 3);

        int moveIndex = bot.chooseBestMove(board, 3, players);
        assertEquals("Should choose pawn that can enter home stretch", 0, moveIndex);
    }

    private int findNearestSafeSpot(Board board, int position) {
        for (int i = 0; i < board.getTotalSpaces(); i++) {
            int spot = (position + i) % board.getTotalSpaces();
            if (board.isSafeSpot(spot)) {
                return spot;
            }
        }
        return position; // Fallback
    }

    @Test
    public void testInvalidMoves() {
        // Test with invalid dice roll
        int moveIndex = bot.chooseBestMove(board, 0, players);
        assertEquals("Should return -1 for invalid dice roll", -1, moveIndex);

        moveIndex = bot.chooseBestMove(board, 7, players);
        assertEquals("Should return -1 for invalid dice roll", -1, moveIndex);
    }

    @Test
    public void testProgressTowardsGoal() {
        // Set up two pawns, one closer to goal
        Pawn pawn1 = bot.getPawns().get(0);
        Pawn pawn2 = bot.getPawns().get(1);

        pawn1.leaveHome(board);
        pawn2.leaveHome(board);

        int startPos = board.getStartPosition(bot.getColor());
        pawn1.setPosition(startPos);
        pawn2.setPosition((startPos + 20) % board.getTotalSpaces());

        // Bot should prefer moving the pawn closer to goal
        int moveIndex = bot.chooseBestMove(board, 3, players);
        assertEquals("Should choose pawn closer to goal", 1, moveIndex);
    }
}
