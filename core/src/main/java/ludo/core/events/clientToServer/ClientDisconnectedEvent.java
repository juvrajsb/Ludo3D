package ludo.core.events.clientToServer;

import ludo.core.events.Event;
//import ludo.core.network.IConnectionManager;

public class ClientDisconnectedEvent extends Event {
//    private static IConnectionManager connectionManager;
    private static final long serialVersionUID = 1L;

    public ClientDisconnectedEvent() {
        super("CLIENT_DISCONNECTED");
    }

//    public static void setConnectionManager(IConnectionManager manager) {
//        connectionManager = manager;
//    }

    @Override
    public void process() {
//        if (connectionManager != null) {
//            connectionManager.handleDisconnection(this.getConnection().getConnectionID());
//        }
    }
}
