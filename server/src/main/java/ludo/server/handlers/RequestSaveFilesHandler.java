package ludo.server.handlers;

import ludo.core.events.Event;
import ludo.core.events.clientToServer.RequestSaveFilesEvent;
import ludo.core.events.serverToClient.SaveFilesListEvent;
import ludo.core.persistence.GamePersistence;
import ludo.server.state.ServerGameStateManager;

import java.util.List;

import static ludo.server.Server.LOGGER;

public class RequestSaveFilesHandler implements IEventHandler {
    @Override
    public void handle(Event event, ServerGameStateManager gsm) {
        RequestSaveFilesEvent requestEvent = (RequestSaveFilesEvent) event;
        String connectionId = requestEvent.getConnection().getConnectionID();
        LOGGER.info("Received request for save files list from " + connectionId);

        List<String> saveFiles = GamePersistence.listSaveFiles();
        LOGGER.info("Found " + saveFiles.size() + " save files. Sending list to client.");

        gsm.getNetworkHandler().sendToClient(connectionId, new SaveFilesListEvent(saveFiles));
    }
} 