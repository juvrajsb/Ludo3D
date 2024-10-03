package ludo.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CopyOnWriteArrayList;

public class LudoServer {
    private static final int PORT = 5000;
    private static final int TOTAL_PLAYERS = 4;
    private static final int BOT_DELAY_MS = 2000; // 2 seconds delay for bot moves
    private List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private LudoGame game;
    private List<Player> players = new ArrayList<>();
    private int currentPlayerIndex;
    private boolean gameStarted = false;

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Ludo Server started on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clients.add(clientHandler);
                new Thread(clientHandler).start();

                if (clients.size() == TOTAL_PLAYERS && !isGameStarted()) {
                    startGame();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startGame() {
        game = new LudoGame();
        game.initializeGame(players);
        currentPlayerIndex = 0;
        gameStarted = true;

        // Notify all players that the game has started
        broadcastMessage("GAME_STARTED");

        // Send initial game state to all clients
        broadcastGameState();

        // Start the first turn
        nextTurn();
    }

    private void broadcastGameState() {
        String gameState = game.getSerializedGameState();
        broadcastMessage("GAME_STATE " + gameState);
    }

    private void nextTurn() {
        Player currentPlayer = players.get(currentPlayerIndex);
        broadcastMessage("TURN " + currentPlayer.getColor());

        if (currentPlayer instanceof BotPlayer) {
            handleBotTurn((BotPlayer) currentPlayer);
        } else {
            setTurnTimer(currentPlayer);
        }
    }

    private void handleBotTurn(BotPlayer botPlayer) {
        new Thread(() -> {
            try {
                Thread.sleep(BOT_DELAY_MS); // Add a delay to make bot moves feel more natural
                int diceRoll = game.rollDice();
                broadcastMessage("DICE " + botPlayer.getColor() + " " + diceRoll);

                Thread.sleep(BOT_DELAY_MS); // Add another delay before the bot moves
                String move = botPlayer.makeMove(game.getBoard(), diceRoll);
                processMove(botPlayer, move);

                // No need to call endTurn() here as it's called in processMove()
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void setTurnTimer(Player currentPlayer) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> {
            if (players.get(currentPlayerIndex) == currentPlayer) {
                // If it's still the same player's turn after 30 seconds, end their turn
                endTurn();
                broadcastMessage("TURN_TIMEOUT " + currentPlayer.getColor());
            }
            executor.shutdown();
        }, 30, TimeUnit.SECONDS);
    }

    private void endTurn() {
        if (game.isGameOver()) {
            broadcastMessage("GAME_OVER " + game.getWinner().getColor());
            // Clean up and reset the game
            return;
        }

        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        nextTurn();
    }

    public void addPlayer(Player player) {
        if (players.size() < TOTAL_PLAYERS) {
            players.add(player);
            broadcastMessage("PLAYER_JOINED " + player.getColor());
            broadcastLobbyUpdate();

            if (players.size() == TOTAL_PLAYERS) {
                startGame();
            } else if (players.size() >= 2 && !isGameStarted()) {
                // Add a bot player after a short delay if we don't have enough human players
                new Thread(() -> {
                    try {
                        Thread.sleep(5000); // Wait 5 seconds before adding a bot
                        if (players.size() < TOTAL_PLAYERS && !isGameStarted()) {
                            addBotPlayer();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        }
    }

    private void addBotPlayer() {
        if (players.size() < TOTAL_PLAYERS) {
            BotPlayer botPlayer = new BotPlayer("Bot" + (players.size() + 1), getNextAvailableColor());
            players.add(botPlayer);
            broadcastMessage("PLAYER_JOINED " + botPlayer.getColor() + " (Bot)");

            if (players.size() == TOTAL_PLAYERS) {
                startGame();
            }
        }
    }

    String getNextAvailableColor() {
        String[] colors = {"RED", "BLUE", "GREEN", "YELLOW"};
        for (String color : colors) {
            if (players.stream().noneMatch(p -> p.getColor().equals(color))) {
                return color;
            }
        }
        return null; // This should never happen if we limit players to 4
    }

    public boolean canAddPlayer() {
        return players.size() < 4 && !isGameStarted();
    }

    public boolean isCurrentPlayer(Player player) {
        return players.get(currentPlayerIndex) == player;
    }

    public void processMove(Player player, String move) {
        try {
            if (game.makeMove(player, move)) {
                broadcastMessage("MOVE " + player.getColor() + " " + move);
                broadcastGameState();
                endTurn();
            } else {
                player.getClientHandler().sendMessage("ERROR Invalid move");
            }
        } catch (IllegalArgumentException e) {
            player.getClientHandler().sendMessage("ERROR " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error processing move: " + e.getMessage());
            e.printStackTrace();
            player.getClientHandler().sendMessage("ERROR Unexpected error occurred");
        }
    }

    public void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public LudoGame getGame() {
        return game;
    }

    private void handleClientDisconnection(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        Player disconnectedPlayer = clientHandler.getPlayer();
        if (disconnectedPlayer != null) {
            players.remove(disconnectedPlayer);
            if (isGameStarted()) {
                // Replace the disconnected player with a bot
                BotPlayer botPlayer = new BotPlayer("Bot" + disconnectedPlayer.getColor(), disconnectedPlayer.getColor());
                players.add(botPlayer);
                broadcastMessage("PLAYER_REPLACED " + disconnectedPlayer.getColor() + " BOT");
            } else {
                broadcastMessage("PLAYER_LEFT " + disconnectedPlayer.getColor());
            }
            broadcastLobbyUpdate();
        }
    }

    private void broadcastLobbyUpdate() {
        StringBuilder playerList = new StringBuilder();
        for (Player player : players) {
            playerList.append(player.getName()).append(",");
        }
        if (playerList.length() > 0) {
            playerList.setLength(playerList.length() - 1); // Remove last comma
        }
        broadcastMessage("LOBBY_UPDATE " + playerList.toString());
    }

    private void handleChatMessage(Player sender, String message) {
        broadcastMessage("CHAT " + sender.getName() + ": " + message);
    }

    public static void main(String[] args) {
        new LudoServer().start();
    }

    public void removePlayer(Player player) {
        players.remove(player);
        broadcastMessage("PLAYER_LEFT " + player.getColor());
        broadcastLobbyUpdate();
    }

    private boolean isGameStarted() {
        return gameStarted;
    }
}
