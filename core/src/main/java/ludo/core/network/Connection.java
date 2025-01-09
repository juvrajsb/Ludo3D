package ludo.core.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Connection {
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final String id;

    public Connection(Socket socket, String id) throws IOException {
        this.socket = socket;
        this.id = id;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    public Connection(String host, int port) {
    }

    public void send(NetworkMessage message) throws IOException {
        synchronized(out) {
            out.writeObject(message.serialize());
            out.flush();
        }
    }

    public NetworkMessage receive() throws IOException, ClassNotFoundException {
        return NetworkMessage.deserialize((byte[])in.readObject());
    }

    public boolean isFailed() {
    }

    public void decrementPingFailure() {
    }

    public String getHost() {
    }

    public int getPort() {
        return 0;
    }

    public void close() {

    }

    public String getConnectionID() {
        return null;
    }
}
