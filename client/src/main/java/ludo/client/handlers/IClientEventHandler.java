package ludo.client.handlers;

import ludo.client.GameStateManager;
import ludo.core.events.Event;

/**
 * Interface for handling specific game events on the client side.
 */
public interface IClientEventHandler {
    void handle(GameStateManager gsm, Event event);
}
