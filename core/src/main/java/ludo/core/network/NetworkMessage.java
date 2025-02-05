package ludo.core.network;

import java.io.*;

public interface NetworkMessage extends Serializable {
    /**
     * Gets the type identifier for this message
     */
    String getType();
}
