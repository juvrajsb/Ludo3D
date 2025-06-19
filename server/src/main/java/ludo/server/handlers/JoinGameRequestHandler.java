package ludo.server.handlers;

import ludo.core.entities.Player;
import ludo.core.events.Event;
import ludo.core.events.clientToServer.JoinGameRequestEvent;
import ludo.core.events.serverToClient.*;
import ludo.server.state.ServerGameStateManager;

import static ludo.server.Server.LOGGER;

public class JoinGameRequestHandler implements IEventHandler {
    @Override
    public void handle(Event event, ServerGameStateManager gsm) {
        JoinGameRequestEvent joinEvent = (JoinGameRequestEvent) event;
        String connectionId = joinEvent.getConnection().getConnectionID();
        String playerName = joinEvent.getPlayerName();
        String color = joinEvent.getDesiredColor().toUpperCase();

        Player disconnectedPlayer = gsm.getGameManager().getPlayerByName(playerName);
        if (disconnectedPlayer != null && disconnectedPlayer.isDisconnected()) {
            LOGGER.info("Found a disconnected player with the name: " + playerName + ". Prompting for reconnect.");
            gsm.getNetworkHandler().sendToClient(connectionId, new ReconnectPromptEvent(playerName));
            return;
        }

        if (isNameTaken(playerName, gsm)) {
            gsm.getNetworkHandler().sendToClient(connectionId, new JoinGameResponseEvent(Response.USERNAME_TAKEN));
            return;
        }
        if (isColorTaken(color, gsm)) {
            gsm.getNetworkHandler().sendToClient(connectionId, new JoinGameResponseEvent(Response.COLOR_TAKEN));
            return;
        }
        if (gsm.getGameManager().getPlayers().size() >= 4) {
            gsm.getNetworkHandler().sendToClient(connectionId, new JoinGameResponseEvent(Response.GAME_FULL));
            return;
        }

        Player newPlayer = new Player(playerName, color);
        if (gsm.getGameManager().addPlayer(newPlayer)) {
            gsm.getConnectionToPlayerMap().put(connectionId, playerName);
            LOGGER.info("Player joined successfully: " + playerName);

            gsm.getNetworkHandler().sendToClient(connectionId, new JoinGameResponseEvent(Response.OK));

            if (gsm.getGameManager().getPlayers().size() == 1) {
                gsm.setAdminPlayerName(playerName);
                gsm.getNetworkHandler().sendToClient(connectionId, new FirstPlayerEvent());
                LOGGER.info("First player joined and set as admin: " + playerName);
            }

            gsm.getNetworkHandler().broadcast(new PlayerJoinedEvent(newPlayer));
            gsm.broadcastPlayerList();
        }
    }

    private boolean isNameTaken(String playerName, ServerGameStateManager gsm) {
        return gsm.getGameManager().getPlayers().stream().anyMatch(p -> p.getName().equalsIgnoreCase(playerName));
    }

    private boolean isColorTaken(String color, ServerGameStateManager gsm) {
        return gsm.getGameManager().getPlayers().stream().anyMatch(p -> p.getColor().equalsIgnoreCase(color));
    }
}
