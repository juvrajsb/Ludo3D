package ludo.core.events.serverToClient;

import ludo.core.events.Event;
import java.util.List;

/**
 * An event sent by the server to the client, containing the list of available save files.
 */
public class SaveFilesListEvent extends Event {
    private static final long serialVersionUID = 1L;
    private final List<String> saveFiles;

    public SaveFilesListEvent(List<String> saveFiles) {
        super("SAVE_FILES_LIST");
        this.saveFiles = saveFiles;
    }

    public List<String> getSaveFiles() {
        return saveFiles;
    }

    @Override
    public void process() {
        // Handled by the client
    }
}
