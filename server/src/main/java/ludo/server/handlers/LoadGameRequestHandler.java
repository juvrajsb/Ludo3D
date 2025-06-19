package ludo.server.handlers;

import ludo.core.events.Event;
import ludo.core.events.clientToServer.LoadGameRequestEvent;
import ludo.core.events.serverToClient.LoadGameResponseEvent;
import ludo.core.persistence.GamePersistence;
import ludo.server.state.ServerGameStateManager;

import static ludo.server.Server.LOGGER;

public class LoadGameRequestHandler implements IEventHandler {
    @Override
    public void handle(Event event, ServerGameStateManager gsm) {
        LoadGameRequestEvent req = (LoadGameRequestEvent) event;
        String connectionId = req.getConnection().getConnectionID();
        if (!gsm.isFirstPlayer(connectionId)) {
            LOGGER.warning("Non-admin player attempted to load game: " + connectionId);
            gsm.getNetworkHandler().sendToClient(
                connectionId,
                new LoadGameResponseEvent(LoadGameResponseEvent.Response.NOT_AUTHORIZED)
            );
            return;
        }

        if (gsm.getGameManager().isGameStarted()) {
            LOGGER.warning("Attempted to load game while one is in progress");
            gsm.getNetworkHandler().sendToClient(
                connectionId,
                new LoadGameResponseEvent(LoadGameResponseEvent.Response.INVALID_STATE)
            );
            return;
        }

        try {
            String fileName = req.isAutoSave() ?
                GamePersistence.AUTO_SAVE_FILE :
                req.getSaveFileName();

            GamePersistence.GameSaveData saveData = GamePersistence.loadGame(fileName);
            if (saveData == null) {
                LOGGER.warning("No save file found at " + fileName);
                gsm.getNetworkHandler().sendToClient(
                    connectionId,
                    new LoadGameResponseEvent(LoadGameResponseEvent.Response.FILE_NOT_FOUND)
                );
                return;
            }

            if (!saveData.isValid()) {
                LOGGER.warning("Invalid save data in file " + fileName);
                gsm.getNetworkHandler().sendToClient(
                    connectionId,
                    new LoadGameResponseEvent(LoadGameResponseEvent.Response.INVALID_SAVE)
                );
                return;
            }

            // You may want to refactor this into a utility method
            // gsm.loadSavedGameState(saveData, connectionId);
            LOGGER.info("Game loaded successfully from " + fileName);

            gsm.getNetworkHandler().sendToClient(
                connectionId,
                new LoadGameResponseEvent(LoadGameResponseEvent.Response.OK, saveData)
            );
        } catch (Exception e) {
            LOGGER.severe("Failed to load game: " + e.getMessage());
            gsm.getNetworkHandler().sendToClient(
                connectionId,
                new LoadGameResponseEvent(LoadGameResponseEvent.Response.SERVER_ERROR, null, e.getMessage())
            );
        }
    }
} 