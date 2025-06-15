//package ludo.test;
//
//import ludo.core.entities.Pawn;
//import ludo.core.entities.Board;
//import org.junit.Before;
//import org.junit.Test;
//import static org.junit.Assert.*;
//
//public class PawnTest {
//    private Pawn pawn;
//    private Board board;
//
//    @Before
//    public void setUp() {
//        pawn = new Pawn("RED");
//        board = new Board();
//    }
//
//    @Test
//    public void testInitialState() {
//        assertTrue("New pawn should be at home", pawn.isHome());
//        assertFalse("New pawn should not be finished", pawn.isFinished());
//        assertEquals("New pawn should have position -1", -1, pawn.getPosition());
//        assertEquals("Pawn should have correct color", "RED", pawn.getColor());
//    }
//
//    @Test
//    public void testMovement() {
//        // Test leaving home
//        pawn.leaveHome(board);
//        assertFalse("Pawn should not be at home after leaving", pawn.isHome());
//        assertEquals("Pawn should be at start position",
//            board.getStartPosition("RED"), pawn.getPosition());
//
//        // Test normal movement
//        pawn.move(3, board);
//        assertEquals("Pawn should move 3 spaces", 3, pawn.getPosition());
//
//        // Test sending home
//        pawn.sendHome();
//        assertTrue("Pawn should be at home after being sent back", pawn.isHome());
//        assertEquals("Position should be -1 when at home", -1, pawn.getPosition());
//    }
//
//    @Test
//    public void testFinishingMove() {
//        // Move pawn to last position before finishing
//        pawn.setPosition(47); // Last regular position for RED
//
//        // Move into home
//        pawn.move(6, board);
////        System.out.println(pawn.getPosition());
//        assertTrue("Pawn should be in home column",
//            board.isHomeColumn(pawn.getPosition(), "RED"));
//
//        // Move to final position
//        pawn.setPosition(57); // Final position
//        pawn.setFinished(true);
//        assertTrue("Pawn should be finished", pawn.isFinished());
//    }
//}
