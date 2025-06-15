//package ludo.test;
//
//import ludo.core.entities.*;
//import ludo.core.game.LudoGame;
//
//import org.junit.Before;
//import org.junit.Test;
//import static org.junit.Assert.*;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class BoardMovementTest {
//    private Board board;
//    private LudoGame game;
//
//    @Before
//    public void setUp() {
//        board = new Board();
//        game = new LudoGame();
//        List<Player> players = new ArrayList<>();
//        players.add(new Player("Player1", "RED"));
//        players.add(new Player("Player2", "GREEN"));
//        players.add(new Player("Player3", "BLUE"));
//        players.add(new Player("Player4", "YELLOW"));
//        game.initializeGame(players);
//    }
//
//    @Test
//    public void testHomeColumnEntry() {
//        Player player = game.getPlayers().get(0);
//        Pawn pawn = player.getPawns().get(0);
//
//        // Move pawn near home column
//        pawn.setPosition(47);
//
//        boolean moveResult = game.movePawn(0, 0, 6);
//
//        assertTrue(moveResult);
//        assertTrue(board.isHomeColumn(pawn.getPosition(), player.getColor()));
//    }
//
//    @Test
//    public void testHomeColumnTracking() {
//        Player player = game.getPlayers().get(0);
//        Pawn pawn = player.getPawns().get(0);
//        // Position 51 should be the last regular board position for RED
//        pawn.setPosition(51);
//
//        // First verify we're not in home column
//        assertFalse("Pawn should not be in home column yet",
//            board.isHomeColumn(pawn.getPosition(), player.getColor()));
//
//        // Now move to home column (52 is first home position for RED)
//        pawn.setPosition(52);
//        assertTrue("Pawn should be in home column",
//            board.isHomeColumn(pawn.getPosition(), player.getColor()));
//    }
//
//    @Test
//    public void testRedPawnHomeEntry() {
//        Player redPlayer = game.getPlayers().get(0); // RED
//        Pawn redPawn = redPlayer.getPawns().get(0);
//        redPawn.leaveHome(game.getBoard());
//        redPawn.setPosition(50);
//
////        System.out.println("Initial position: " + redPawn.getPosition());
////        System.out.println("Start position for RED: " + game.getBoard().getStartPosition("RED"));
////        System.out.println("Attempting to move 4 spaces");
//
//        boolean moveResult = game.movePawn(0, 0, 4);
////        System.out.println("Move result: " + moveResult);
////        System.out.println("New position: " + redPawn.getPosition());
////        System.out.println("Is in home column: " + board.isHomeColumn(redPawn.getPosition(), "RED"));
//
//        assertTrue(moveResult);
//        assertTrue(board.isHomeColumn(redPawn.getPosition(), "RED"));
//    }
//
//    @Test
//    public void testBluePawnWraparound() {
//        Player bluePlayer = game.getPlayers().get(2); // BLUE
//        Pawn bluePawn = bluePlayer.getPawns().get(0);
//        bluePawn.leaveHome(game.getBoard());
//        bluePawn.setPosition(50);
//
////        System.out.println("Initial position: " + bluePawn.getPosition());
////        System.out.println("Start position for BLUE: " + game.getBoard().getStartPosition("BLUE"));
////        System.out.println("Attempting to move 4 spaces");
//
//        boolean moveResult = game.movePawn(2, 0, 4);
////        System.out.println("Move result: " + moveResult);
////        System.out.println("New position: " + bluePawn.getPosition());
//
//        assertTrue(moveResult);
//        assertEquals(2, bluePawn.getPosition());
//    }
//}
