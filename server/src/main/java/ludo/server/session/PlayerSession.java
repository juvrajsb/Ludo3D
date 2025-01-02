package ludo.server.session;

import ludo.core.entities.Player;
import ludo.server.networking.Connection;
import ludo.server.events.Event;
import java.io.IOException;

public class PlayerSession {
    private final Player player;
    private Connection connection;
    private long lastActivityTime;
    private SessionState state;
    private int reconnectAttempts;

    private static final int MAX_RECONNECT_ATTEMPTS = 3;

    public enum SessionState {
        CONNECTED,
        DISCONNECTED,
        RECONNECTING,
        INACTIVE
    }

    public PlayerSession(Player player, Connection connection) {
        this.player = player;
        this.connection = connection;
        this.lastActivityTime = System.currentTimeMillis();
        this.state = SessionState.CONNECTED;
        this.reconnectAttempts = 0;
    }

    public void updateActivity() {
        this.lastActivityTime = System.currentTimeMillis();
    }

    public boolean isActive() {
        return state == SessionState.CONNECTED || state == SessionState.RECONNECTING;
    }

    public void sendEvent(Event event) throws IOException {
        if (connection != null && state == SessionState.CONNECTED) {
            try {
                connection.send(event);
                updateActivity();
            } catch (IOException e) {
                handleConnectionError();
                throw e;
            }
        }
    }

    public void handleConnectionError() {
        if (state == SessionState.CONNECTED) {
            state = SessionState.DISCONNECTED;
            reconnectAttempts = 0;
        }
    }

    public boolean attemptReconnect(Connection newConnection) {
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++;
            if (connection != null) {
                try {
                    connection.close();
                } catch (IOException e) {
                    // Log error but continue with reconnection
                }
            }
            connection = newConnection;
            state = SessionState.CONNECTED;
            updateActivity();
            return true;
        }
        state = SessionState.INACTIVE;
        return false;
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException e) {
                // Log error but continue with disconnection
            }
        }
        state = SessionState.DISCONNECTED;
    }

    // Getters
    public Player getPlayer() {
        return player;
    }

    public Connection getConnection() {
        return connection;
    }

    public SessionState getState() {
        return state;
    }

    public long getLastActivityTime() {
        return lastActivityTime;
    }

    public String getPlayerName() {
        return player.getName();
    }

    public String getPlayerColor() {
        return player.getColor();
    }

    public int getReconnectAttempts() {
        return reconnectAttempts;
    }
}
