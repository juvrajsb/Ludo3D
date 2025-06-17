package ludo.core.events.serverToClient;

import ludo.core.events.Event;

/**
 * Event sent by the controller to notify the client
 * about the status of his joining request.
 */
public class JoinGameResponseEvent extends Event {
    private static final long serialVersionUID = 1L;
    private final Response response;
    private boolean existsSavedGame;

    public JoinGameResponseEvent(Response response) {
        super("JOIN_GAME_RESPONSE");
        this.response = response;
        this.existsSavedGame = false;
    }

    public Response getResponse() {
        return response;
    }

    @Override
    public void process() {
        // Will be processed by client handler
    }

}
