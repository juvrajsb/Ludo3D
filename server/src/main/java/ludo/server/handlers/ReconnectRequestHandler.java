package ludo.server.handlers;

import ludo.core.entities.Player;
import ludo.core.events.Event;
import ludo.core.events.clientToServer.ReconnectRequestEvent;
import ludo.core.events.serverToClient.ErrorEvent;
import ludo.core.events.serverToClient.GameStartedEvent;
import ludo.core.events.serverToClient.GameStateUpdateEvent;
import ludo.core.network.Connection;
import ludo.server.state.ServerGameStateManager;

import static ludo.server.Server.LOGGER;

public class ReconnectRequestHandler implements IEventHandler {
    @Override
    public void handle(Event event, ServerGameStateManager gsm) {
        ReconnectRequestEvent reconnectEvent = (ReconnectRequestEvent) event;
        String playerName = reconnectEvent.getPlayerName();
        Connection newConnection = reconnectEvent.getConnection();
        String newConnectionId = newConnection.getConnectionID();

        Player player = gsm.getGameManager().getPlayerByName(playerName);

        if (player != null && player.isDisconnected()) {
            LOGGER.info("Reconnection successful for player: " + playerName);

            player.setDisconnected(false);
            gsm.getConnectionToPlayerMap().put(newConnectionId, playerName);
            gsm.getNetworkHandler().getServer().getClientConnectionHandler().handleReconnection(playerName, newConnection);

            // Send GameStartedEvent to get them back into the game screen
            gsm.getNetworkHandler().sendToClient(newConnectionId, new GameStartedEvent(
                gsm.getGameManager().getPlayers(),
                gsm.getGameManager().getCurrentPlayer().getName(),
                gsm.getGameManager().getPlayers().size()
            ));

            // Send the latest game state to sync them up
            gsm.broadcastGameState();
        } else {
            gsm.getNetworkHandler().sendToClient(newConnectionId, new ErrorEvent("Could not find a disconnected player with that name."));
            LOGGER.warning("Rejected reconnection attempt for player: " + playerName);
        }
    }
}
