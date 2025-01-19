package ludo.server.networking;

import ludo.core.network.PingSender;
import ludo.core.network.Connection;
import ludo.core.events.serverToClient.PingEvent;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

public class ServerPingSender implements PingSender {
    private static final Logger LOGGER = Logger.getLogger(ServerPingSender.class.getName());
    private final Connection connection;
    private Timer timer;
    private TimerTask pingTask;

    public ServerPingSender(Connection connection) {
        this.connection = connection;
        this.timer = null;
        this.pingTask = null;
    }

    @Override
    public boolean sendPing() {
        try {
            connection.send(new PingEvent());
            connection.decrementPingFailure();
            return true;
        } catch (Exception e) {
            LOGGER.warning("Failed to send ping to " + connection.getConnectionID() + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    public void start() {
        if (timer != null) {
            stop();
        }

        timer = new Timer("Server-Ping-" + connection.getConnectionID());
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
