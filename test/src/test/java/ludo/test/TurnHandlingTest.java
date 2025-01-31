package ludo.test;

import ludo.client.GameStateManager;
import ludo.client.networking.ClientNetworkHandler;
import ludo.client.screens.GameScreen;
import ludo.core.events.serverToClient.TurnChangeEvent;
import ludo.core.events.serverToClient.DiceRollResultEvent;
import ludo.core.events.clientToServer.DiceRollRequestEvent;
import ludo.core.events.clientToServer.MoveRequestEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class TurnHandlingTest {
    @Mock
    private ClientNetworkHandler networkHandler;

    @Mock
    private GameScreen gameScreen;

    private GameStateManager gameStateManager;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // Mock network connection status
        when(networkHandler.isConnected()).thenReturn(true);

        gameStateManager = new GameStateManager();

        // Inject mocks
        try {
            java.lang.reflect.Field networkField = GameStateManager.class.getDeclaredField("networkHandler");
            networkField.setAccessible(true);
            networkField.set(gameStateManager, networkHandler);

            // Initialize with mock GameScreen
            gameStateManager.initialize(gameScreen);
        } catch (Exception e) {
            fail("Could not set up mocks");
        }
    }

    @Test
    public void testPlayerTurn() {
        // Setup initial state
        gameStateManager.joinGame("Player1", "RED");

        // Simulate turn change to this player
        TurnChangeEvent turnEvent = new TurnChangeEvent("Player1");
        gameStateManager.onMessageReceived(turnEvent);

        // Try to roll dice - should succeed
        gameStateManager.requestDiceRoll();
        verify(networkHandler).sendMessage(any(DiceRollRequestEvent.class));
        verify(gameScreen).enableControls();
    }

    @Test
    public void testNotPlayerTurn() {
        // Setup initial state
        gameStateManager.joinGame("Player1", "RED");

        // Simulate turn change to other player
        TurnChangeEvent turnEvent = new TurnChangeEvent("Player2");
        gameStateManager.onMessageReceived(turnEvent);

        // Try to roll dice - should not send message
        gameStateManager.requestDiceRoll();
        verify(networkHandler, never()).sendMessage(any(DiceRollRequestEvent.class));
        verify(gameScreen).disableControls();
    }

    @Test
    public void testMoveOnlyAfterDiceRoll() {
        // Setup player turn
        gameStateManager.joinGame("Player1", "RED");
        gameStateManager.onMessageReceived(new TurnChangeEvent("Player1"));

        // Simulate dice roll
        DiceRollResultEvent diceEvent = new DiceRollResultEvent(6, "RED");
        gameStateManager.handleDiceRoll(diceEvent);

        // Verify dice display was updated
        verify(gameScreen).updateDiceDisplay(6);

        // Try to move - should succeed
        gameStateManager.requestMove(0);
        verify(networkHandler).sendMessage(any(MoveRequestEvent.class));
    }

    @Test
    public void testNoMoveBeforeDiceRoll() {
        // Setup player turn
        gameStateManager.joinGame("Player1", "RED");
        gameStateManager.onMessageReceived(new TurnChangeEvent("Player1"));

        // Try to move without rolling - should not send message
        gameStateManager.requestMove(0);
        verify(networkHandler, never()).sendMessage(any(MoveRequestEvent.class));
    }
}

//public class TurnHandlingTest {
//    @Mock
//    private ClientNetworkHandler networkHandler;
//    @Mock
//    private GameScreen gameScreen;
//
//    private GameStateManager gameStateManager;
//
//    @Before
//    public void setup() {
//        MockitoAnnotations.initMocks(this);
//        gameStateManager = new GameStateManager(networkHandler);
//        gameStateManager.initialize(gameScreen);
//
//        // Setup initial state
//        when(networkHandler.isConnected()).thenReturn(true);
//        gameStateManager.joinGame("Player1", "RED");
//    }
//
//    @Test
//    public void testPlayerTurnStart() {
//        // When it becomes player's turn
//        TurnChangeEvent turnEvent = new TurnChangeEvent("Player1");
//        gameStateManager.onMessageReceived(turnEvent);
//
//        // Should enable controls and show message
//        verify(gameScreen).enableControls();
//        verify(gameScreen).showMessage("Your turn!");
//    }
//
//    @Test
//    public void testOtherPlayerTurn() {
//        // When it becomes another player's turn
//        TurnChangeEvent turnEvent = new TurnChangeEvent("Player2");
//        gameStateManager.onMessageReceived(turnEvent);
//
//        // Should disable controls and show waiting message
//        verify(gameScreen).disableControls();
//        verify(gameScreen).showMessage("Waiting for Player2");
//    }
//
//    @Test
//    public void testDiceRollOnlyOnPlayerTurn() {
//        // Not player's turn
//        TurnChangeEvent otherTurn = new TurnChangeEvent("Player2");
//        gameStateManager.onMessageReceived(otherTurn);
//
//        // Try to roll dice
//        gameStateManager.requestDiceRoll();
//        verify(networkHandler, never()).sendMessage(any(DiceRollRequestEvent.class));
//
//        // Now player's turn
//        TurnChangeEvent playerTurn = new TurnChangeEvent("Player1");
//        gameStateManager.onMessageReceived(playerTurn);
//
//        // Try to roll dice again
//        gameStateManager.requestDiceRoll();
//        verify(networkHandler).sendMessage(any(DiceRollRequestEvent.class));
//    }
//
//    @Test
//    public void testMoveOnlyOnPlayerTurn() {
//        // Set current dice value
//        gameStateManager.handleDiceRoll(/* create a DiceRollResultEvent with value 6 */);
//
//        // Not player's turn
//        TurnChangeEvent otherTurn = new TurnChangeEvent("Player2");
//        gameStateManager.onMessageReceived(otherTurn);
//
//        // Try to move
//        gameStateManager.requestMove(0);
//        verify(networkHandler, never()).sendMessage(any(MoveRequestEvent.class));
//
//        // Now player's turn
//        TurnChangeEvent playerTurn = new TurnChangeEvent("Player1");
//        gameStateManager.onMessageReceived(playerTurn);
//
//        // Try to move again
//        gameStateManager.requestMove(0);
//        verify(networkHandler).sendMessage(any(MoveRequestEvent.class));
//    }
//}
