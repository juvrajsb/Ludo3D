package ludo.server.handlers;

import ludo.core.events.clientToServer.DiceRollRequestEvent;
import ludo.core.events.serverToClient.DiceRollResultEvent;
import ludo.core.game.GameManager;
import ludo.server.networking.EventTransmitter;

public class DiceRollRequestHandler {
    private GameManager gameManager;
    private EventTransmitter eventTransmitter;

    public DiceRollRequestHandler() {
//        this.gameManager = GameManager.getInstance();
//        this.eventTransmitter = new EventTransmitter(Server.getInstance().getAllConnections());
    }
//TODO check

//    public void handle(DiceRollRequestEvent event) {
//        Player currentPlayer = gameManager.getCurrentPlayer();
//
//        // Validate if it's the player's turn
//        if (!currentPlayer.getName().equals(event.getConnection().getConnectionID())) {
//            Server.LOGGER.warning("Player attempted to roll dice out of turn: " +
//                event.getConnection().getConnectionID());
//            return;
//        }
//
//        // Roll the dice
//        int rollValue = gameManager.rollDice();
//
//        // Create and broadcast result event
//        DiceRollResultEvent resultEvent = new DiceRollResultEvent(
//            rollValue,
//            currentPlayer.getColor()
//        );
//
//        try {
//            eventTransmitter.broadcast(resultEvent);
//        } catch (Exception e) {
//            Server.LOGGER.severe("Failed to broadcast dice roll result: " + e.getMessage());
//        }
//    }

    public void process (DiceRollRequestEvent event) {
        // Validate if it's player's turn
        if (!gameManager.isPlayerTurn(event.getConnection().getConnectionID())) {
            return;
        }

        // Roll dice
        int rollValue = gameManager.rollDice();

        // Broadcast result
        DiceRollResultEvent resultEvent = new DiceRollResultEvent(
            rollValue,
            gameManager.getCurrentPlayer().getColor()
        );
        eventTransmitter.broadcast(resultEvent);
    }
}
