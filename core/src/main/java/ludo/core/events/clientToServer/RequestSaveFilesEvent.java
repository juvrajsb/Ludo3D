package ludo.core.events.clientToServer;

import ludo.core.events.Event;

/**
 * An event sent by a client to request a list of available save files from the server.
 */
public class RequestSaveFilesEvent extends Event {
    private static final long serialVersionUID = 1L;

    public RequestSaveFilesEvent() {
        super("REQUEST_SAVE_FILES");
    }

    @Override
    public void process() {
        // Handled by the server
    }
}
