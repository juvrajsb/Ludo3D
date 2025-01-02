package ludo.server.events.serverToClient;

import ludo.server.events.Event;

/**
 * Event sent by the controller to notify the client
 * about the status of his joining request.
 */
public class JoinGameResponseEvent extends Event {
    private final Response response;
    private boolean existsSavedGame;

    public JoinGameResponseEvent(Response response) {
        ID = "JOIN_GAME_RESPONSE_EVENT";
        this.response = response;
        this.existsSavedGame = false;
    }

    /**
     * Method used client side to check if the client has been accepted in the game
     * @return {@code true} if the player has joined successfully, {@code false} otherwise
     */
    public Response getResponse() {
        return response;
    }

    public void setExistsSavedGame(boolean existsSavedGame) {
        this.existsSavedGame = existsSavedGame;
    }

    public boolean existsSavedGame() {
        return existsSavedGame;
    }

    @Override
    public void callHandler() {
        new JoinGameResponseHandler().handle(this);
    }
}
