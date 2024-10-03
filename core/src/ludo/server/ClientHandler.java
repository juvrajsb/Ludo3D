package src.ludo.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

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
                // Process client messages and update game state
                processMessage(inputLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
            player = new Player(playerName, this);
            server.addPlayer(player);
            sendMessage("JOINED " + player.getColor());
        } else {
            sendMessage("ERROR Game is full or already started");
        }
    }

    private void handleDiceRoll() {
        if (server.isCurrentPlayer(player)) {
            int roll = server.getGame().rollDice();
            server.broadcastMessage("DICE " + player.getColor() + " " + roll);
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
}