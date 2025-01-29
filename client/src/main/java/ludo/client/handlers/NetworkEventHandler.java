package ludo.client.handlers;

import ludo.client.networking.ClientNetworkHandler;
import ludo.core.events.clientToServer.*;
import ludo.core.network.Connection;
/**
 * This class is responsible for handling the network events.
 */
public class NetworkEventHandler {
    private final ClientNetworkHandler client;
    private final Connection serverConnection;

    public NetworkEventHandler(Connection serverConnection, ClientNetworkHandler client) {
        this.client = client;
//        this.client = Client.getInstance();
        this.serverConnection = serverConnection;
    }

    public void handleDisconnection() {
        // Handle controller disconnection
        client.stop(); //TODO check
        // Show disconnection screen or return to menu
    }

    public void handleReconnection() {
        try {
            client.start(serverConnection.getConnectionID(), //TODO check
                Integer.parseInt(System.getProperty("controller.port", "12000")));
        } catch (Exception e) {
            System.err.println("Failed to reconnect: " + e.getMessage());
        }
    }

    public void handlePing() {
        // Send pong response to keep connection alive
        try {
            serverConnection.send(new PongEvent());
        } catch (Exception e) {
            System.err.println("Failed to send pong: " + e.getMessage());
        }
    }

    public void sendJoinRequest(String playerName, String desiredColor) {
        try {
            JoinGameRequestEvent joinEvent = new JoinGameRequestEvent(playerName, desiredColor);
            serverConnection.send(joinEvent);
        } catch (Exception e) {
            System.err.println("Failed to send join request: " + e.getMessage());
        }
    }

    public void sendDiceRollRequest() {
        try {
            serverConnection.send(new DiceRollRequestEvent());
        } catch (Exception e) {
            System.err.println("Failed to send dice roll request: " + e.getMessage());
        }
    }

    public void sendMoveRequest(int pawnIndex, int steps) {
        try {
            MoveRequestEvent moveEvent = new MoveRequestEvent(pawnIndex, steps);
            serverConnection.send(moveEvent);
        } catch (Exception e) {
            System.err.println("Failed to send move request: " + e.getMessage());
        }
    }

    public void sendStartGameRequest() {
        try {
            serverConnection.send(new StartGameRequestEvent());
        } catch (Exception e) {
            System.err.println("Failed to send start game request: " + e.getMessage());
        }
    }
}
