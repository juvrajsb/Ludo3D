package ludo.client.networking;

import ludo.core.network.PingSender;
import ludo.core.network.Connection;
import ludo.core.events.clientToServer.PongEvent;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

public class ClientPingSender implements PingSender {
    private static final Logger LOGGER = Logger.getLogger(ClientPingSender.class.getName());
    private final Connection connection;
    private Timer timer;
    private TimerTask pingTask;

    public ClientPingSender(Connection connection) {
        this.connection = connection;
    }

    @Override
    public boolean sendPing() {
        try {
            connection.send(new PongEvent());
            connection.decrementPingFailure();
            return true;
        } catch (Exception e) {
            LOGGER.warning("Failed to send pong: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void start() {
        if (timer != null) {
            stop();
        }

        timer = new Timer("Client-Ping");
        pingTask = new TimerTask() {
            @Override
            public void run() {
                if (!sendPing() || connection.isFailed()) {
                    stop();
                }
            }
        };

        timer.scheduleAtFixedRate(pingTask, 0, PING_PERIOD);
    }

    @Override
    public void stop() {
        if (pingTask != null) {
            pingTask.cancel();
            pingTask = null;
        }
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }
}
