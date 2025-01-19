package ludo.core.events;

import ludo.core.network.NetworkMessage;

public abstract class GameEvent implements NetworkMessage {
    private final String eventType;
    private final long timestamp;

    protected GameEvent(String eventType) {
        this.eventType = eventType;
        this.timestamp = System.currentTimeMillis();
    }

    public String getEventType() {
        return eventType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public abstract void process();

        @Override
    public String getType() {
        return eventType;
    }
}
