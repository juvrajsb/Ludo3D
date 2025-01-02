package ludo.server.events;

import ludo.server.networking.Connection;
import java.io.Serializable;

public abstract class Event implements Serializable {
    protected String ID;
    protected transient Connection connection;

    public String getID() {
        return ID;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return connection;
    }

    /**
     * Method that calls the appropriate handler for this event
     */
    public abstract void callHandler();

    @Override
    public String toString() {
        return "Event{ID='" + ID + "'}";
    }
}
