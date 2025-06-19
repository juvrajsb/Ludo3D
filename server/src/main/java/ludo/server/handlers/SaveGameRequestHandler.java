package ludo.server.handlers;

import ludo.core.events.Event;
import ludo.core.events.clientToServer.SaveGameRequestEvent;
import ludo.core.events.serverToClient.SaveGameResponseEvent;
import ludo.core.persistence.GamePersistence;
import ludo.server.state.ServerGameStateManager;

import java.io.IOException;

import static ludo.server.Server.LOGGER;

public class SaveGameRequestHandler implements IEventHandler {
    @Override
    public void handle(Event event, ServerGameStateManager gsm) {
        SaveGameRequestEvent req = (SaveGameRequestEvent) event;
        String connectionId = req.getConnection().getConnectionID();
        if (!gsm.isFirstPlayer(connectionId)) {
            LOGGER.warning("Non-admin player attempted to save game: " + connectionId);
            gsm.getNetworkHandler().sendToClient(
                connectionId,
                new SaveGameResponseEvent(SaveGameResponseEvent.Response.NOT_AUTHORIZED)
            );
            return;
        }

        if (!gsm.getGameManager().isGameStarted()) {
            LOGGER.warning("Attempted to save game before it started");
            gsm.getNetworkHandler().sendToClient(
                connectionId,
                new SaveGameResponseEvent(SaveGameResponseEvent.Response.INVALID_STATE)
            );
            return;
        }

        try {
            String fileName = req.isAutoSave() ?
                GamePersistence.AUTO_SAVE_FILE :
                GamePersistence.SAVE_FILE;

            GamePersistence.GameSaveData saveData = new GamePersistence.GameSaveData();
            saveData.players = gsm.getGameManager().getPlayers().stream()
                .map(player -> {
                    GamePersistence.PlayerSaveData playerData = new GamePersistence.PlayerSaveData();
                    playerData.name = player.getName();
                    playerData.color = player.getColor();
                    playerData.pawns = player.getPawns().stream()
                        .map(pawn -> {
                            GamePersistence.PawnSaveData pawnData = new GamePersistence.PawnSaveData();
                            pawnData.position = pawn.getPosition();
                            pawnData.isHome = pawn.isHome();
                            pawnData.isFinished = pawn.isFinished();
                            return pawnData;
                        })
                        .toList();
                    return playerData;
                })
                .toList();
            saveData.currentPlayerColor = gsm.getGameManager().getCurrentPlayer().getColor();
            saveData.gameState = gsm.getGameManager().getGameState();

            GamePersistence.saveGame(saveData, fileName);
            LOGGER.info("Game saved successfully to " + fileName);

            gsm.getNetworkHandler().sendToClient(
                connectionId,
                new SaveGameResponseEvent(SaveGameResponseEvent.Response.OK, fileName)
            );
        } catch (IOException e) {
            LOGGER.severe("Failed to save game: " + e.getMessage());
            gsm.getNetworkHandler().sendToClient(
                connectionId,
                new SaveGameResponseEvent(SaveGameResponseEvent.Response.SAVE_FAILED, null, e.getMessage())
            );
        }
    }
} 