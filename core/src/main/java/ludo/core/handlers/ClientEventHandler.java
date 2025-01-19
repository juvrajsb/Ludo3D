package ludo.core.handlers;

import ludo.core.events.serverToClient.*;

// Client-side event handler
public interface ClientEventHandler extends EventHandler {
    // Game state events
    void handleGameStateUpdate(GameStateUpdateEvent event);

    void handleGameStart(GameStartEvent event);

    void handleGameOver(GameOverEvent event);

    // Game action results
    void handleDiceRollResult(DiceRollResultEvent event);

    void handleMoveResult(MoveResultEvent event);

    // Player events
    void handlePlayerJoined(PlayerJoinedEvent event);

    void handlePlayerLeft(PlayerLeftEvent event);

    // Connection events
    void handleConnectionLost();

    void handleReconnected();

    void handlePing(PingEvent event);
}
