package ludo.core.events.serverToClient;

import ludo.core.events.Event;

public class ErrorEvent extends Event {
    private static final long serialVersionUID = 1L;
    private final String message;

    public ErrorEvent(String message) {
        super("ERROR");
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public void process() {
        // Will be processed by client handler
    }
}
