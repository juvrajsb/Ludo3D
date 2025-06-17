package ludo.server.networking;

import ludo.core.network.PingSender;
import ludo.core.network.Connection;
import ludo.core.events.serverToClient.PingEvent;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerPingSender implements PingSender {
    private static final Logger LOGGER = Logger.getLogger(ServerPingSender.class.getName());
    private final Connection connection;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Timer timer;
    private static final long PING_PERIOD = 5000; // 5 seconds

    public ServerPingSender(Connection connection) {
        this.connection = connection;
        this.timer = null;
    }

    @Override
    public boolean sendPing() {
        if (!running.get() || connection.isClosed()) {
            return false;
        }

        try {
            connection.send(new PingEvent());
            connection.decrementPingFailure();
            return true;
        } catch (Exception e) {
            if (running.get()) {
                LOGGER.warning("Failed to send ping to " + connection.getConnectionID() + ": " + e.getMessage());
                connection.incrementPingFailure();
                if (connection.isFailed()) {
                    LOGGER.warning("Too many ping failures for " + connection.getConnectionID() + ", disconnecting");
                    stop();
                    return false;
                }
            }
            return false;
        }
    }

    @Override
    public void start() {
        if (running.get()) {
            return;
        }

        running.set(true);
        timer = new Timer("Server-Ping-" + connection.getConnectionID());

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!running.get() || connection.isClosed() || connection.isFailed()) {
                    stop();
                    return;
                }

                try {
                    if (!sendPing()) {
                        connection.incrementPingFailure();
                        if (connection.isFailed()) {
                            stop();
                        }
                    }
                } catch (Exception e) {
                    if (running.get()) {
                        LOGGER.warning("Ping failed for " + connection.getConnectionID());
                        connection.incrementPingFailure();
                        if (connection.isFailed()) {
                            stop();
                        }
                    }
                }
            }
        }, 0, PING_PERIOD);
    }

    @Override
    public void stop() {
        if (!running.getAndSet(false)) {
            return;
        }

        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }
}
