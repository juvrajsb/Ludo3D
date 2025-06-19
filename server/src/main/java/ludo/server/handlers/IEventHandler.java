package ludo.server.handlers;

import ludo.core.events.Event;
import ludo.server.state.ServerGameStateManager;

/**
 * Interface for handling specific game events.
 */
public interface IEventHandler {
    void handle(Event event, ServerGameStateManager gameStateManager);
} 