package ludo.test;

import ludo.client.networking.ClientNetworkHandler;
import ludo.client.GameStateManager;
import ludo.core.events.clientToServer.DiceRollRequestEvent;
import ludo.core.events.clientToServer.JoinGameRequestEvent;
import ludo.core.network.*;
import ludo.core.events.serverToClient.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class GameStateIntegrationTest {
    @Mock
    private ClientNetworkHandler networkHandler;
    private GameStateManager gameState;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(networkHandler.isConnected()).thenReturn(true);
        gameState = new GameStateManager(networkHandler); // Use the constructor with networkHandler
    }

    @Test
    public void testJoinGameFlow() {
        // Test joining game
        String username = "TestPlayer";
        String color = "RED";

        // Setup networkHandler mock behavior
        when(networkHandler.isConnected()).thenReturn(true);

        // Should successfully join when connected
        assertTrue(gameState.joinGame(username, color));

        // Verify username and color are stored
        assertEquals(username, gameState.getCurrentUsername());
        assertEquals(color, gameState.getCurrentColor());

        // Verify join request was sent
        verify(networkHandler).sendMessage(any(JoinGameRequestEvent.class));
    }

    @Test
    public void testTurnHandling() {
        // Setup initial state
        String username = "TestPlayer";
        when(networkHandler.getPlayerId()).thenReturn(username);
        gameState.joinGame(username, "RED");

        // Simulate receiving turn change event with the player's username
        TurnChangeEvent turnEvent = new TurnChangeEvent(username);
        gameState.onMessageReceived(turnEvent);

        // Request dice roll - this will only work if it's the player's turn
        gameState.requestDiceRoll();

        // Verify that dice roll request was sent (indicating it was player's turn)
        verify(networkHandler).sendMessage(any(DiceRollRequestEvent.class));
    }
}
