package ludo.server.handlers;

import ludo.core.entities.Pawn;
import ludo.core.entities.Player;
import ludo.core.events.Event;
import ludo.core.events.clientToServer.MoveRequestEvent;
import ludo.core.events.serverToClient.CanRollAgainEvent;
import ludo.core.events.serverToClient.MoveResultEvent;
import ludo.core.events.serverToClient.TurnChangeEvent;
import ludo.core.game.GameState;
import ludo.server.state.ServerGameStateManager;
import static ludo.server.Server.LOGGER;

public class MoveRequestHandler implements IEventHandler {
    @Override
    public void handle(Event event, ServerGameStateManager gsm) {
        MoveRequestEvent moveEvent = (MoveRequestEvent) event;
        String connectionId = moveEvent.getConnection().getConnectionID();
        String playerName = gsm.getConnectionToPlayerMap().get(connectionId);

        LOGGER.info("Received MOVE_REQUEST from connectionId=" + connectionId + ", playerName=" + playerName + ", pawnIndex=" + moveEvent.getPawnIndex() + ", steps=" + moveEvent.getSteps());

        if (!gsm.getGameManager().isPlayerTurn(playerName)) {
            LOGGER.warning("Move rejected: Not player's turn. Current player: " + gsm.getGameManager().getCurrentPlayer().getName() + ", Requesting player: " + playerName);
            sendMoveResponse(connectionId, false, "Not your turn", moveEvent.getPawnIndex(), -1, gsm);
            return;
        }

        Player currentPlayer = gsm.getGameManager().getPlayerByName(playerName);
        if (currentPlayer == null) {
            LOGGER.warning("Move rejected: Player not found in game manager: " + playerName);
            sendMoveResponse(connectionId, false, "Player not found", moveEvent.getPawnIndex(), -1, gsm);
            return;
        }

        int steps = moveEvent.getSteps();
        if (steps != gsm.getGameManager().getLastDiceRoll()) {
            LOGGER.warning("Move rejected: Dice roll mismatch. steps=" + steps + ", lastDiceRoll=" + gsm.getGameManager().getLastDiceRoll());
            sendMoveResponse(connectionId, false, "Invalid move: Dice roll mismatch", moveEvent.getPawnIndex(), -1, gsm);
            return;
        }

        int playerIndex = gsm.getGameManager().getPlayers().indexOf(currentPlayer);
        int pawnIndex = moveEvent.getPawnIndex();

        LOGGER.info("Attempting to move pawn. playerIndex=" + playerIndex + ", pawnIndex=" + pawnIndex + ", steps=" + steps);
        boolean moveSuccess = gsm.getGameManager().movePawn(playerIndex, pawnIndex, steps);

        if (moveSuccess) {
            LOGGER.info("Move successful for player=" + playerName + ", pawnIndex=" + pawnIndex + ", steps=" + steps);
            handleMoveSuccess(connectionId, pawnIndex, steps, gsm);
        } else {
            LOGGER.warning("Move failed for pawn " + pawnIndex + " (player=" + playerName + ", steps=" + steps + ")");
            Pawn selectedPawn = currentPlayer.getPawns().get(pawnIndex);
            String failureReason = getFailureReason(selectedPawn, steps);
            sendMoveResponse(connectionId, false, failureReason, pawnIndex, -1, gsm);
            gsm.getGameManager().setGameState(GameState.WAITING_FOR_MOVE);
            gsm.broadcastGameState();
        }
    }

    private void handleMoveSuccess(String connectionId, int pawnIndex, int steps, ServerGameStateManager gsm) {
        int newPosition = gsm.getGameManager().getCurrentPlayer().getPawns().get(pawnIndex).getPosition();
        LOGGER.info("handleMoveSuccess: Broadcasting MoveResultEvent for pawnIndex=" + pawnIndex + ", newPosition=" + newPosition);
        MoveResultEvent resultEvent = new MoveResultEvent(true, "Move successful", pawnIndex, newPosition);
        gsm.getNetworkHandler().broadcast(resultEvent);
        gsm.getGameManager().setGameState(GameState.PLAYER_MOVED);

        // Check win condition
        if (gsm.getGameManager().hasPlayerWon(gsm.getGameManager().getCurrentPlayer().getName())) {
            LOGGER.info("Player has won: " + gsm.getGameManager().getCurrentPlayer().getName());
            gsm.handleGameOver(gsm.getGameManager().getCurrentPlayer().getName());
            return;
        }

        // Check for extra roll conditions
        boolean extraRoll = steps == 6 || gsm.getGameManager().wasPawnCaptured() || gsm.getGameManager().getCurrentPlayer().getPawns().get(pawnIndex).isFinished();
        LOGGER.info("Extra roll? " + extraRoll + " (steps==6: " + (steps==6) + ", wasPawnCaptured: " + gsm.getGameManager().wasPawnCaptured() + ", pawnFinished: " + gsm.getGameManager().getCurrentPlayer().getPawns().get(pawnIndex).isFinished() + ")");

        if (extraRoll) {
            gsm.getGameManager().setGameState(GameState.IN_PROGRESS);
            gsm.getGameManager().setLastDiceRoll(0);
            gsm.getNetworkHandler().broadcast(new TurnChangeEvent(gsm.getGameManager().getCurrentPlayer().getName()));
            gsm.getNetworkHandler().sendToClient(connectionId, new CanRollAgainEvent());
        } else {
            gsm.getGameManager().nextTurn();
            String nextPlayer = gsm.getGameManager().getCurrentPlayer().getName();
            LOGGER.info("Turn changes to next player: " + nextPlayer);
            gsm.getNetworkHandler().broadcast(new TurnChangeEvent(nextPlayer));
        }

        // Always broadcast updated game state after a move
        LOGGER.info("Broadcasting updated game state after move.");
        gsm.broadcastGameState();

        gsm.checkAndPlayBotTurn();
    }

    private void sendMoveResponse(String connectionId, boolean success, String message, int pawnIndex, int newPosition, ServerGameStateManager gsm) {
        MoveResultEvent response = new MoveResultEvent(success, message, pawnIndex, newPosition);
        gsm.getNetworkHandler().sendToClient(connectionId, response);
    }

    private String getFailureReason(Pawn pawn, int steps) {
        if (pawn.isHome() && steps != 6) return "Cannot leave home without a 6";
        if (pawn.isFinished()) return "Pawn has already reached home";
        return "Invalid move";
    }
}
