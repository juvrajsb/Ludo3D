package ludo.server.session;

import ludo.core.entities.Player;
import ludo.server.networking.Connection;
import ludo.server.events.Event;
import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SessionManager {
    private static SessionManager instance;
    private final Map<String, PlayerSession> sessions;
    private static final long SESSION_TIMEOUT = 300000; // 5 minutes

    private SessionManager() {
        this.sessions = new ConcurrentHashMap<>();
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public PlayerSession createSession(Player player, Connection connection) {
        PlayerSession session = new PlayerSession(player, connection);
        sessions.put(player.getName(), session);
        return session;
    }

    public void removeSession(String playerName) {
        PlayerSession session = sessions.remove(playerName);
        if (session != null) {
            session.disconnect();
        }
    }

    public void broadcastEvent(Event event) {
        List<String> failedSessions = new ArrayList<>();

        for (PlayerSession session : sessions.values()) {
            if (session.isActive()) {
                try {
                    session.sendEvent(event);
                } catch (IOException e) {
                    failedSessions.add(session.getPlayerName());
                }
            }
        }

        // Clean up failed sessions
        failedSessions.forEach(this::handleFailedSession);
    }

    public void handleFailedSession(String playerName) {
        PlayerSession session = sessions.get(playerName);
        if (session != null) {
            session.handleConnectionError();
            // Additional error handling can be added here
        }
    }

    public boolean reconnectPlayer(String playerName, Connection newConnection) {
        PlayerSession session = sessions.get(playerName);
        if (session != null) {
            return session.attemptReconnect(newConnection);
        }
        return false;
    }

    public void cleanupInactiveSessions() {
        long currentTime = System.currentTimeMillis();
        List<String> inactivePlayers = sessions.values().stream()
            .filter(session -> (currentTime - session.getLastActivityTime()) > SESSION_TIMEOUT)
            .map(PlayerSession::getPlayerName)
            .collect(Collectors.toList());

        inactivePlayers.forEach(this::removeSession);
    }

    public List<Player> getActivePlayers() {
        return sessions.values().stream()
            .filter(PlayerSession::isActive)
            .map(PlayerSession::getPlayer)
            .collect(Collectors.toList());
    }

    public PlayerSession getSession(String playerName) {
        return sessions.get(playerName);
    }

    public boolean isPlayerActive(String playerName) {
        PlayerSession session = sessions.get(playerName);
        return session != null && session.isActive();
    }

    public int getActiveSessionCount() {
        return (int) sessions.values().stream()
            .filter(PlayerSession::isActive)
            .count();
    }

    public void shutdown() {
        sessions.values().forEach(PlayerSession::disconnect);
        sessions.clear();
    }
}
