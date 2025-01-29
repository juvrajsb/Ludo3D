package ludo.server;

import ludo.server.Server;

public class ServerLauncher {
    public static void main(String[] args) {
        Server server = Server.getInstance();

        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }
}
