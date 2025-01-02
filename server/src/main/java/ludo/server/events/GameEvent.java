package ludo.server.events;

public abstract class GameEvent {
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
}
