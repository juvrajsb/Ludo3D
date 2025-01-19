package ludo.server.state;

import ludo.core.entities.*;
import ludo.core.game.GameState;
import ludo.core.events.serverToClient.*;
import ludo.core.utils.Constants;
import ludo.core.validation.MoveValidator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerGameStateManager {
    private final Map<String, Player> players;
    private final Board board;
    private final Dice dice;
    private String currentPlayerId;
    private GameState gameState;
    private final int minPlayers = Constants.MIN_PLAYERS;
    private final int maxPlayers = Constants.MAX_PLAYERS;

    public ServerGameStateManager() {
        this.players = new ConcurrentHashMap<>();
        this.board = new Board();
        this.dice = new Dice();
        this.gameState = GameState.WAITING_FOR_PLAYERS;
    }

    // Game flow control
    public boolean addPlayer(String playerId, String color) {
        if (players.size() >= maxPlayers || gameState != GameState.WAITING_FOR_PLAYERS) {
            return false;
        }

        Player newPlayer = new Player(playerId, color);
        players.put(playerId, newPlayer);
        return true;
    }

    public void removePlayer(String playerId) {
        players.remove(playerId);
        if (players.size() < minPlayers) {
            gameState = GameState.WAITING_FOR_PLAYERS;
        }
    }

    public boolean startGame() {
        if (players.size() >= minPlayers && gameState == GameState.WAITING_FOR_PLAYERS) {
            gameState = GameState.IN_PROGRESS;
            currentPlayerId = players.keySet().iterator().next(); // First player starts
            return true;
        }
        return false;
    }

    // Game actions
    public int rollDice() {
        return dice.roll();
    }

    public boolean movePawn(String playerId, int pawnIndex, int steps) {
        if (!isPlayerTurn(playerId)) {
            return false;
        }

        Player player = players.get(playerId);
        if (player == null) {
            return false;
        }

        // Validate and execute move
        Pawn pawn = player.getPawns().get(pawnIndex);
        if (MoveValidator.isValidMove(player, pawn, steps, board)) {
            pawn.move(steps, board);
            handleCaptures(pawn.getPosition());
            return true;
        }

        return false;
    }

    private void handleCaptures(int position) {
        if (!board.isSafeSpot(position)) {
            for (Player player : players.values()) {
                for (Pawn pawn : player.getPawns()) {
                    if (pawn.getPosition() == position) {
                        pawn.sendHome();
                    }
                }
            }
        }
    }

    public void nextTurn() {
        List<String> playerIds = new ArrayList<>(players.keySet());
        int currentIndex = playerIds.indexOf(currentPlayerId);
        currentPlayerId = playerIds.get((currentIndex + 1) % playerIds.size());
    }

    // State queries
    public boolean isPlayerTurn(String playerId) {
        return currentPlayerId != null && currentPlayerId.equals(playerId);
    }

    public Map<String, List<Integer>> getPawnPositions() {
        Map<String, List<Integer>> positions = new HashMap<>();
        for (Player player : players.values()) {
            List<Integer> playerPositions = new ArrayList<>();
            for (Pawn pawn : player.getPawns()) {
                playerPositions.add(pawn.getPosition());
            }
            positions.put(player.getColor(), playerPositions);
        }
        return positions;
    }

    public boolean hasPlayerWon(String playerId) {
        Player player = players.get(playerId);
        return player != null && player.getPawns().stream()
            .allMatch(Pawn::isFinished);
    }

    public Collection<Player> getPlayers() {
        return Collections.unmodifiableCollection(players.values());
    }

    public String getCurrentPlayerId() {
        return currentPlayerId;
    }

    public GameState getGameState() {
        return gameState;
    }
}
