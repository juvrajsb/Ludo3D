package ludo.core.network;

public interface NetworkHandler {
    void connect(String host, int port);
    void disconnect();
    void sendMessage(NetworkMessage message);
    void setMessageListener(MessageListener listener);
}

