package ludo.core.events.serverToClient;

import ludo.core.events.Event;

public class StartGameResponseEvent extends Event {
    private static final long serialVersionUID = 1L;
    private final boolean success;
    private final String message;
    private final Response response;

    public enum Response {
        OK("Game started successfully"),
        NOT_ENOUGH_PLAYERS("Not enough players to start the game"),
        GAME_ALREADY_STARTED("Game has already started"),
        NOT_ADMIN("Only the admin can start the game");

        private final String message;

        Response(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public StartGameResponseEvent(Response response) {
        super("START_GAME_RESPONSE");
        this.response = response;
        this.success = response == Response.OK;
        this.message = response.getMessage();
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Response getResponse() {
        return response;
    }

    @Override
    public void process() {
        // Will be processed by client handler
    }
}
