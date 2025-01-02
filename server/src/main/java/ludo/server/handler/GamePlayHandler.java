package ludo.server.handler;

import ludo.server.events.clientToServer.*;
import ludo.server.events.serverToClient.*;
import ludo.server.game.GameManager;
import ludo.server.networking.EventTransmitter;
import ludo.entities.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GamePlayHandler extends BaseHandler {
    private final GameManager gameManager;
    private final EventTransmitter eventTransmitter;

    public GamePlayHandler() {
        this.gameManager = GameManager.getInstance();
        this.eventTransmitter = new EventTransmitter(Server.getInstance().getAllConnections());
    }

    public void handleDiceRoll(DiceRollRequestEvent event) {
        Player currentPlayer = gameManager.getCurrentPlayer();
        String playerColor = currentPlayer.getColor();

        // Check if it's the player's turn
        if (!currentPlayer.getName().equals(event.getConnection().getConnectionID())) {
            return; // Not your turn
        }

        // Roll the dice
        int rollValue = gameManager.rollDice();

        // Broadcast the result to all players
        DiceRollResultEvent resultEvent = new DiceRollResultEvent(rollValue, playerColor);
        eventTransmitter.broadcast(resultEvent);

        // Update game state
        updateGameState();
    }

    public void handleMove(MoveRequestEvent event) {
        Player currentPlayer = gameManager.getCurrentPlayer();

        // Validate player's turn
        if (!currentPlayer.getName().equals(event.getConnection().getConnectionID())) {
            return;
        }

        // Try to make the move
        boolean moveSuccess = gameManager.movePawn(
            currentPlayer,
            event.getPawnIndex(),
            event.getSteps()
        );

        // Send move result
        MoveResultEvent resultEvent = new MoveResultEvent(
            moveSuccess,
            moveSuccess ? "Move successful" : "Invalid move",
            event.getPawnIndex(),
            moveSuccess ? currentPlayer.getPawnPosition(event.getPawnIndex()) : -1
        );
        eventTransmitter.broadcast(resultEvent);

        if (moveSuccess) {
            // Check for game over
            if (gameManager.hasPlayerWon(currentPlayer)) {
                handleGameOver(currentPlayer);
            } else {
                gameManager.nextTurn();
                updateGameState();
            }
        }
    }

    public void handleGameStart(StartGameRequestEvent event) {
        if (gameManager.getPlayers().size() >= 2) {
            gameManager.startGame();

            GameStartEvent startEvent = new GameStartEvent(
                gameManager.getPlayers(),
                gameManager.getCurrentPlayer().getName()
            );
            eventTransmitter.broadcast(startEvent);

            updateGameState();
        }
    }

    private void handleGameOver(Player winner) {
        WinnerProclamationEvent winEvent = new WinnerProclamationEvent(
            List.of(winner.getName())
        );
        eventTransmitter.broadcast(winEvent);
    }

    private void updateGameState() {
        // Collect current pawn positions for all players
        Map<String, List<Integer>> pawnPositions = new HashMap<>();
        for (Player player : gameManager.getPlayers()) {
            List<Integer> positions = player.getPawns().stream()
                .map(pawn -> pawn.getPosition())
                .collect(Collectors.toList());
            pawnPositions.put(player.getColor(), positions);
        }

        // Create and broadcast game state update
        GameStateUpdateEvent updateEvent = new GameStateUpdateEvent(
            pawnPositions,
            gameManager.getCurrentPlayer().getColor(),
            gameManager.getGameState()
        );
        eventTransmitter.broadcast(updateEvent);
    }
}
