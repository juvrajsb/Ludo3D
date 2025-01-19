package ludo.core.network;

public interface MessageListener {
    void onMessageReceived(NetworkMessage message);
    void onConnectionError(Exception e);
}
