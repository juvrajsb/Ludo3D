package ludo.server.handler;

import ludo.server.events.clientToServer.DiceRollRequestEvent;
import ludo.server.events.serverToClient.DiceRollResultEvent;
import ludo.server.game.GameManager;
import ludo.server.networking.EventTransmitter;
import ludo.server.Server;
import ludo.core.entities.Player;

public class DiceRollRequestHandler extends BaseHandler {
    private final GameManager gameManager;
    private final EventTransmitter eventTransmitter;

    public DiceRollRequestHandler() {
        this.gameManager = GameManager.getInstance();
        this.eventTransmitter = new EventTransmitter(Server.getInstance().getAllConnections());
    }

    public void handle(DiceRollRequestEvent event) {
        Player currentPlayer = gameManager.getCurrentPlayer();

        // Validate if it's the player's turn
        if (!currentPlayer.getName().equals(event.getConnection().getConnectionID())) {
            Server.LOGGER.warning("Player attempted to roll dice out of turn: " +
                event.getConnection().getConnectionID());
            return;
        }

        // Roll the dice
        int rollValue = gameManager.rollDice();

        // Create and broadcast result event
        DiceRollResultEvent resultEvent = new DiceRollResultEvent(
            rollValue,
            currentPlayer.getColor()
        );

        try {
            eventTransmitter.broadcast(resultEvent);
        } catch (Exception e) {
            Server.LOGGER.severe("Failed to broadcast dice roll result: " + e.getMessage());
        }
    }
}
