package ludo.core.network;

import java.io.*;

/**
 * Interface for all network messages in the game.
 * Messages are used for communication between client and server.
 */
public interface NetworkMessage extends Serializable {
    /**
     * Gets the type identifier for this message
     */
    String getType();
}
