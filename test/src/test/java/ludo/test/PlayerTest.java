//package ludo.test;
//
//import ludo.core.entities.Player;
//import ludo.core.entities.Pawn;
//import ludo.core.entities.Board;
//import org.junit.Before;
//import org.junit.Test;
//import static org.junit.Assert.*;
//
//public class PlayerTest {
//    private Player player;
//    private Board board;
//
//    @Before
//    public void setUp() {
//        player = new Player("TestPlayer", "RED");
//        board = new Board();
//    }
//
//    @Test
//    public void testPlayerInitialization() {
//        assertEquals("Player name should match", "TestPlayer", player.getName());
//        assertEquals("Player color should match", "RED", player.getColor());
//        assertEquals("Player should have 4 pawns", 4, player.getPawns().size());
//
//        // Test initial pawn states
//        for (Pawn pawn : player.getPawns()) {
//            assertTrue("All pawns should start at home", pawn.isHome());
//            assertEquals("All pawns should have position -1", -1, pawn.getPosition());
//        }
//    }
//
//    @Test
//    public void testHomeColumnTracking() {
//        // Get a pawn and move it to just before home column
//        Pawn pawn = player.getPawns().get(0);
//        pawn.setPosition(51); // Last position before RED home column
//
//        assertFalse("Pawn should not be in home column yet",
//            board.isHomeColumn(pawn.getPosition(), player.getColor()));
//
//        // Move into home column
//        pawn.setPosition(52); // First home column position
//        assertTrue("Pawn should be in home column",
//            board.isHomeColumn(pawn.getPosition(), player.getColor()));
//    }
//
//    @Test
//    public void testPawnMovement() {
//        int pawnIndex = 0;
//        assertEquals("Initial position should be -1",
//            -1, player.getPawnPosition(pawnIndex));
//
//        // Set new position
//        player.setPawnPosition(pawnIndex, 5);
//        assertEquals("Position should be updated",
//            5, player.getPawnPosition(pawnIndex));
//    }
//}
