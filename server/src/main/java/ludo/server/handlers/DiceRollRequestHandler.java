package ludo.server.handlers;

import ludo.core.events.Event;
import ludo.core.events.clientToServer.DiceRollRequestEvent;
import ludo.core.events.serverToClient.DiceRollResultEvent;
import ludo.core.events.serverToClient.ErrorEvent;
import ludo.core.game.GameState;
import ludo.server.state.ServerGameStateManager;

import java.util.Timer;
import java.util.TimerTask;

import static ludo.server.Server.LOGGER;

public class DiceRollRequestHandler implements IEventHandler {
    @Override
    public void handle(Event event, ServerGameStateManager gsm) {
        DiceRollRequestEvent rollEvent = (DiceRollRequestEvent) event;
        String connectionId = rollEvent.getConnection().getConnectionID();
        String playerName = gsm.getConnectionToPlayerMap().get(connectionId);

        if (playerName == null) {
            sendErrorResponse(connectionId, "Player not found", gsm);
            return;
        }
        if (gsm.getGameManager().getGameState() != GameState.IN_PROGRESS) {
            sendErrorResponse(connectionId, "Game not in progress", gsm);
            return;
        }
        if (!gsm.getGameManager().isPlayerTurn(playerName)) {
            sendErrorResponse(connectionId, "Not your turn", gsm);
            return;
        }
        if (gsm.getGameManager().getLastDiceRoll() > 0) {
            sendErrorResponse(connectionId, "You have already rolled", gsm);
            return;
        }

        int diceValue = gsm.getGameManager().rollDice();
        gsm.getGameManager().setGameState(GameState.DICE_ROLLED);

        DiceRollResultEvent resultEvent = new DiceRollResultEvent(diceValue, gsm.getGameManager().getCurrentPlayer().getColor());
        gsm.getNetworkHandler().broadcast(resultEvent);

        if (!gsm.getGameManager().hasValidMovesAvailable(gsm.getGameManager().getCurrentPlayer(), diceValue)) {
            LOGGER.info("No valid moves available - automatically ending turn");
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    new TurnEndHandler().handle(event, gsm);
                    timer.cancel();
                }
            }, 1500);
        } else {
            gsm.getGameManager().setGameState(GameState.WAITING_FOR_MOVE);
            gsm.broadcastGameState();
        }
    }

    private void sendErrorResponse(String connectionId, String message, ServerGameStateManager gsm) {
        ErrorEvent errorEvent = new ErrorEvent(message);
        gsm.getNetworkHandler().sendToClient(connectionId, errorEvent);
    }
}
