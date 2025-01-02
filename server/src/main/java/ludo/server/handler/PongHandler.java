package ludo.server.handler;

import ludo.server.events.clientToServer.PongEvent;
import ludo.server.networking.Connection;
import ludo.server.Server;

public class PongHandler extends BaseHandler {
    private final ClientConnectionHandler connectionHandler;

    public PongHandler() {
        this.connectionHandler = new ClientConnectionHandler();
    }

    public void handle(PongEvent event) {
        Connection connection = event.getConnection();
        if (connection != null) {
            // Reset ping failure counter as we received a pong
            connection.resetPingFailure();

            // If connection was marked as failing, log recovery
            if (connection.isFailed()) {
                Server.LOGGER.info("Connection recovered for client: " +
                    connection.getConnectionID());
            }
        }
    }

    public void handleMissedPong(Connection connection) {
        if (connection == null) return;

        // Decrement ping failure counter
        connection.decrementPingFailure();

        // If we've reached max failures, handle disconnection
        if (connection.isFailed()) {
            Server.LOGGER.warning("Connection failed for client: " +
                connection.getConnectionID());
            connectionHandler.handleUnexpectedDisconnection(
                connection.getConnectionID()
            );
        }
    }
}
