package ludo.core.events;

import ludo.core.network.Connection;
import ludo.core.network.NetworkMessage;


public abstract class Event implements NetworkMessage {
    protected String ID;
    protected transient Connection connection;

    protected Event(String ID) {
        this.ID = ID;
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

    public abstract void process();
}
