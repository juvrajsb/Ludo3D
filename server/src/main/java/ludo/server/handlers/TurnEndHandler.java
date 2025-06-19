package ludo.server.handlers;

import ludo.core.events.Event;
import ludo.core.events.serverToClient.TurnChangeEvent;
import ludo.server.state.ServerGameStateManager;

import static ludo.server.Server.LOGGER;

public class TurnEndHandler implements IEventHandler {
    @Override
    public void handle(Event event, ServerGameStateManager gsm) {
        String connectionId = event.getConnection().getConnectionID();
        String playerName = gsm.getConnectionToPlayerMap().get(connectionId);

        if (playerName != null && gsm.getGameManager().isPlayerTurn(playerName)) {
            LOGGER.info("Turn ended for player: " + playerName);

            gsm.getGameManager().nextTurn();
            TurnChangeEvent turnEvent = new TurnChangeEvent(
                gsm.getGameManager().getCurrentPlayer().getName()
            );
            gsm.getNetworkHandler().broadcast(turnEvent);

            gsm.broadcastGameState();

            // This requires checkAndPlayBotTurn to be public in ServerGameStateManager.
            try {
                java.lang.reflect.Method m = gsm.getClass().getDeclaredMethod("checkAndPlayBotTurn");
                m.setAccessible(true);
                m.invoke(gsm);
            } catch (Exception ignored) {}
        }
    }
}
