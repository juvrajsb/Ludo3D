package ludo.core.handlers;

import ludo.core.events.Event;

// Base interface for all event handlers
public interface EventHandler {
    void handleEvent(Event event);
}

