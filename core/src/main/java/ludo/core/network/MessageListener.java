package ludo.core.network;

/**
 * Interface for classes that want to receive network messages.
 */
public interface MessageListener {
    void onMessageReceived(NetworkMessage message);
    void onConnectionError(Exception e);
}
