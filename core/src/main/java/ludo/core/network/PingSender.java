package ludo.core.network;

/**
 * Base interface for ping mechanism to monitor connection health
 */
public interface PingSender {
    int PING_PERIOD = 2000; // 2 seconds

    /**
     * Called when ping should be sent
     * @return true if ping was sent successfully
     */
    boolean sendPing();

    /**
     * Stop sending pings
     */
    void stop();

    /**
     * Start sending pings
     */
    void start();

}
