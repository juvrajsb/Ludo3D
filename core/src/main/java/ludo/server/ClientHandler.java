package ludo.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.stream.Stream;

public class ClientHandler implements Runnable {
    private Socket socket;
    private LudoServer server;
    private PrintWriter out;
    private BufferedReader in;
    private Player player;

    public ClientHandler(Socket socket, LudoServer server) {
        this.socket = socket;
        this.server = server;
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                processMessage(inputLine);
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private void processMessage(String message) {
        String[] parts = message.split(" ");
        String command = parts[0];

        switch (command) {
            case "JOIN":
                handleJoinRequest(parts[1]); // parts[1] is the player's name
                break;
            case "ROLL":
                handleDiceRoll();
                break;
            case "MOVE":
                handleMove(parts[1]); // parts[1] is the move details
                break;
            case "CHAT":
                handleChatMessage(message.substring(5)); // Remove "CHAT " prefix
                break;
            default:
                sendMessage("ERROR Invalid command");
        }
    }

    private void handleJoinRequest(String playerName) {
        if (server.canAddPlayer()) {
            String color = server.getNextAvailableColor();
            if (color != null) {
                player = new Player(playerName, color);
                player.setClientHandler(this);
                server.addPlayer(player);
                sendMessage("JOINED " + player.getColor());
            } else {
                sendMessage("ERROR No available colors");
            }
        } else {
            sendMessage("ERROR Game is full or already started");
        }
    }

    private void handleDiceRoll() {
        if (server.isCurrentPlayer(player)) {
            int[] rolls = new int[]{server.getGame().rollDice()};
            String rollString = Arrays.stream(rolls)
                                      .mapToObj(String::valueOf)
                                      .reduce((a, b) -> a + " " + b)
                                      .orElse("");
            server.broadcastMessage("DICE " + player.getColor() + " " + rollString);
        } else {
            sendMessage("ERROR Not your turn");
        }
    }

    private void handleMove(String moveDetails) {
        if (server.isCurrentPlayer(player)) {
            server.processMove(player, moveDetails);
        } else {
            sendMessage("ERROR Not your turn");
        }
    }

    private void handleChatMessage(String chatMessage) {
        server.broadcastMessage("CHAT " + player.getColor() + " " + chatMessage);
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    private void disconnect() {
        try {
            if (player != null) {
                server.removePlayer(player);
            }
            socket.close();
        } catch (IOException e) {
            System.err.println("Error closing client connection: " + e.getMessage());
        }
    }

    public Player getPlayer() {
        return player;
    }
}
