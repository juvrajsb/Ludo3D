package ludo.core.events.serverToClient;

//import ludo.core.events.serverToClient.JoinGameResponseEvent;

/**
 * Enum containing all the possible responses
 */
public enum Response {
    /**
     * Player accepted in the waiting room.
     */
    OK,

    /**
     * Player accepted in controller's queue.
     */
    WAIT,

    /**
     * Player connected for first to the controller.
     */
    FIRST_PLAYER,

    /**
     * Connection refused because another player
     * with the same username is already connected
     * to controller.
     */
    USERNAME_TAKEN,

    /**
     * Connection refused because the number of
     * players in the controller's connections queue
     * has already reached four.
     */
    QUEUE_FULL,

    /**
     * Connection refused because the game
     * is starting and there are already
     * enough players inside.
     */
    GAME_FULL,

    /**
     * Connection refused because the first
     * player connected to the controller decided
     * to resume a previous game, but it did
     * not take part in it.
     */
    NOT_A_SAVED_GAME_PLAYER
}
