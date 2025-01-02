package ludo.server.events.clientToServer;

import ludo.server.events.Event;
import ludo.server.handler.GamePlayHandler;

// Event for game start request
public class StartGameRequestEvent extends Event {
    public StartGameRequestEvent() {
        ID = "START_GAME_REQUEST";
    }

    @Override
    public void callHandler() {
        new GamePlayHandler().handleGameStart(this);
    }
}
