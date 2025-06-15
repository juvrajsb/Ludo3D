//package ludo.test;
//
//import ludo.client.GameStateManager;
//import ludo.client.networking.ClientNetworkHandler;
//import ludo.core.network.*;
//import org.junit.Before;
//import org.junit.Test;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//
//import static org.junit.Assert.*;
//
//public class NetworkIntegrationTest {
//    @Mock private MessageListener messageListener;
//    private ClientNetworkHandler networkHandler;
//    private GameStateManager gameState;
//
//    @Before
//    public void setup() {
//        MockitoAnnotations.initMocks(this);
//        networkHandler = new ClientNetworkHandler();
//        networkHandler.setMessageListener(messageListener);
//        gameState = new GameStateManager(networkHandler);
//    }
//
//    @Test
//    public void testConnectionEstablishment() {
//        // Should connect successfully to local server
//        networkHandler.connect("localhost", 12000);
//        assertTrue(networkHandler.isConnected());
//    }
//
//    @Test
//    public void testReconnection() {
//        networkHandler.connect("localhost", 12000);
//        // Simulate disconnect
//        networkHandler.disconnect();
//        assertFalse(networkHandler.isConnected());
//        // Should reconnect
//        networkHandler.connect("localhost", 12000);
//        assertTrue(networkHandler.isConnected());
//    }
//
//    @Test
//    public void testPingPongCycle() throws Exception {
//        networkHandler.connect("localhost", 12000);
//        // Wait for ping cycle
//        Thread.sleep(2100); // Just over 2 seconds (default ping interval)
//        // Verify connection still alive
//        assertTrue(networkHandler.isConnected());
//    }
//}
//
