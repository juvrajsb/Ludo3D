package ludo.server.handlers;

import ludo.core.events.Event;
import ludo.server.state.ServerGameStateManager;

import static ludo.server.Server.LOGGER;

public class PongHandler implements IEventHandler {
    @Override
    public void handle(Event event, ServerGameStateManager gsm) {
        String connectionId = event.getConnection().getConnectionID();
        event.getConnection().resetPingFailure();
        LOGGER.fine("Received pong from client: " + connectionId);
    }
} 