package ludo.server.handlers;

import ludo.core.events.clientToServer.JoinGameRequestEvent;
import ludo.core.events.serverToClient.JoinGameResponseEvent;
import ludo.core.events.serverToClient.Response;
import ludo.core.game.GameManager;
import ludo.core.entities.Player;
import ludo.server.networking.EventTransmitter;
import ludo.server.Server;

public class ClientJoinHandler{
    private final GameManager gameManager;
    private final EventTransmitter eventTransmitter;

    public ClientJoinHandler() {
        this.gameManager = GameManager.getInstance();
        this.eventTransmitter = new EventTransmitter(Server.getInstance().getAllConnections());
    }

    public void handle(JoinGameRequestEvent event) {
        // Validate player count
        if (gameManager.getPlayers().size() >= 4) {
            sendResponse(event, Response.QUEUE_FULL);
            return;
        }

        // Check if game already started
        if (gameManager.isGameStarted()) {
            sendResponse(event, Response.GAME_FULL);
            return;
        }

        // Check if color is available
        if (!isColorAvailable(event.getDesiredColor())) {
            sendResponse(event, Response.USERNAME_TAKEN);
            return;
        }

        // Create and add new player
        Player newPlayer = new Player(event.getPlayerName(), event.getDesiredColor());
        if (gameManager.addPlayer(newPlayer)) {
            // Set connection ID for the player
            event.getConnection().setConnectionID(event.getPlayerName());

            // Send success response
            if (gameManager.getPlayers().size() == 1) {
                sendResponse(event, Response.FIRST_PLAYER);
            } else {
                sendResponse(event, Response.OK);
            }

            // Update waiting room for all players
            updateWaitingRoom();
        }
    }

    private boolean isColorAvailable(String color) {
        return gameManager.getPlayers().stream()
            .noneMatch(p -> p.getColor().equalsIgnoreCase(color));
    }

    private void sendResponse(JoinGameRequestEvent event, Response response) {
        JoinGameResponseEvent responseEvent = new JoinGameResponseEvent(response);
        try {
            event.getConnection().send(responseEvent);
        } catch (Exception e) {
            Server.LOGGER.severe("Failed to send join response: " + e.getMessage());
        }
    }

    private void updateWaitingRoom() {
        // Implementation for updating waiting room state
        // This would broadcast current player list to all connected clients
    }
}
