package ludo.core.network;

/**
 * Base interface for ping mechanism to monitor connection health
 */
public interface PingSender {
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
