package ludo.core.events;

import ludo.core.network.Connection;
import ludo.core.network.NetworkMessage;

import java.io.Serializable;


public abstract class Event implements NetworkMessage, Serializable {
    private static final long serialVersionUID = 1L;
    protected String ID;
    protected transient Connection connection;
    private int messageId;
    private long timestamp;

    protected Event(String ID) {
        this.ID = ID;
        this.timestamp = System.currentTimeMillis();
    }
    @Override
    public String getType() {
        return ID;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return connection;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public abstract void process();
}
