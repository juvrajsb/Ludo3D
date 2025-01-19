package ludo.core.handlers;

import ludo.core.events.clientToServer.*;
import ludo.core.network.Connection;

// Server-side event handler
public interface ServerEventHandler extends EventHandler {
    // Game events
    void handleDiceRollRequest(DiceRollRequestEvent event);

    void handleMoveRequest(MoveRequestEvent event);

    void handleStartGameRequest(StartGameRequestEvent event);

    // Player management
    void handleJoinRequest(JoinGameRequestEvent event);

    void handleLeaveRequest(LeaveGameRequestEvent event);

    // Connection events
    void handlePlayerDisconnected(Connection connection);

    void handlePlayerReconnected(Connection connection);

    void handlePong(PongEvent event);
}
