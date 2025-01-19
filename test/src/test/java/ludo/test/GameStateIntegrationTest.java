package ludo.test;

import ludo.client.networking.ClientNetworkHandler;
import ludo.client.state.ClientGameStateManager;
import ludo.core.network.NetworkHandler;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class GameStateIntegrationTest {
    private ClientGameStateManager gameState;
    private NetworkHandler networkHandler;

    @Before
    public void setup() {
        networkHandler = new ClientNetworkHandler();
        gameState = new ClientGameStateManager(networkHandler);
    }

    @Test
    public void testGameStateSync() {
        networkHandler.connect("localhost", 12000);
        gameState.joinGame("TestPlayer", "RED");
        // Verify initial state
        assertNotNull(gameState.getLocalPlayer());
        assertEquals("TestPlayer", gameState.getLocalPlayer().getName());
    }

    @Test
    public void testTurnManagement() {
        networkHandler.connect("localhost", 12000);
        gameState.joinGame("TestPlayer", "RED");

        // Should not allow moves when not player's turn
        assertFalse(gameState.isLocalPlayerTurn());
        gameState.requestDiceRoll(); // Should not send request
        verify(networkHandler, never()).sendMessage(any());
    }
}
