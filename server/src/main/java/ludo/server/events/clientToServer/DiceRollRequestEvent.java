package ludo.server.events.clientToServer;

import ludo.server.events.Event;
import ludo.server.handler.GamePlayHandler;

public class DiceRollRequestEvent extends Event {
    public DiceRollRequestEvent() {
        ID = "DICE_ROLL_REQUEST";
    }

    @Override
    public void callHandler() {
        new GamePlayHandler().handleDiceRoll(this);
    }
}

