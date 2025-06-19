package ludo.server.handlers;

import ludo.core.entities.Player;
import ludo.core.events.Event;
import ludo.core.events.clientToServer.LeaveGameRequestEvent;
import ludo.core.events.serverToClient.PlayerLeftEvent;
import ludo.server.state.ServerGameStateManager;

import static ludo.server.Server.LOGGER;

public class LeaveGameRequestHandler implements IEventHandler {
    @Override
    public void handle(Event event, ServerGameStateManager gsm) {
        LeaveGameRequestEvent leaveEvent = (LeaveGameRequestEvent) event;
        String connectionId = leaveEvent.getConnection().getConnectionID();

        String playerName = gsm.getConnectionToPlayerMap().remove(connectionId);
        if (playerName != null) {
            // If game has not started, remove the player completely.
            if (!gsm.getGameManager().isGameStarted()) {
                gsm.getGameManager().removePlayer(playerName);
            } else {
                // If game is running, find the player and mark them as disconnected.
                Player player = gsm.getGameManager().getPlayerByName(playerName);
                if (player != null) {
                    player.setDisconnected(true);
                    LOGGER.info("Player '" + playerName + "' marked as disconnected.");
                    // Notify other clients that this player has left.
                    gsm.getNetworkHandler().broadcast(new PlayerLeftEvent(playerName));
                }
            }
        }
        // Update the player list for everyone.
        gsm.broadcastPlayerList();
    }
}
