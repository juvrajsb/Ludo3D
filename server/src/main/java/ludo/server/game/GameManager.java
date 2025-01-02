package ludo.server.game;

import ludo.core.entities.Pawn;
import ludo.core.entities.Player;
import ludo.core.entities.Board;
import ludo.core.entities.Dice;
import ludo.core.utils.Constants;
import ludo.server.networking.Connection;
import ludo.server.events.serverToClient.GameStateUpdateEvent;
import ludo.core.validation.MoveValidator;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class GameManager {
    private static GameManager instance;
    private final List<Player> players;
    private final Board board;
    private final Dice dice;
    private int currentPlayerIndex;
    private GameState gameState;
    private boolean gameStarted;
    private final Map<String, Player> playerMap; // For quick player lookups

    private GameManager() {
        this.players = new CopyOnWriteArrayList<>();
        this.board = new Board();
        this.dice = new Dice();
        this.currentPlayerIndex = 0;
        this.gameState = GameState.WAITING_FOR_PLAYERS;
        this.gameStarted = false;
        this.playerMap = new HashMap<>();
    }

    public static synchronized GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    public synchronized boolean addPlayer(Player player) {
        if (players.size() >= Constants.MAX_PLAYERS || gameStarted) {
            return false;
        }
        players.add(player);
        playerMap.put(player.getName(), player);
        return true;
    }

    public synchronized void startGame() {
        if (players.size() >= 2) {
            gameStarted = true;
            gameState = GameState.IN_PROGRESS;
            currentPlayerIndex = 0;
            initializePlayerPositions();
        }
    }

    public synchronized boolean movePawn(Player player, int pawnIndex, int steps) {
        if (!isValidMove(player, pawnIndex, steps)) {
            return false;
        }

        // Execute the move
        boolean moveSuccess = player.getPawns().get(pawnIndex).move(steps, board);

        if (moveSuccess) {
            checkForCaptures(player, player.getPawns().get(pawnIndex).getPosition());
            return true;
        }
        return false;
    }

    private void checkForCaptures(Player movingPlayer, int position) {
        if (board.isSafeSpot(position)) {
            return;
        }

        for (Player otherPlayer : players) {
            if (otherPlayer != movingPlayer) {
                otherPlayer.getPawns().stream()
                    .filter(p -> p.getPosition() == position)
                    .forEach(p -> p.sendHome());
            }
        }
    }

    private boolean isValidMove(Player player, int pawnIndex, int steps) {
        if (player != getCurrentPlayer() ||
            pawnIndex < 0 ||
            pawnIndex >= Constants.PAWNS_PER_PLAYER) {
            return false;
        }

        Pawn pawn = player.getPawns().get(pawnIndex);
        return MoveValidator.isValidMove(player, pawn, steps, board);
    }

    public synchronized void nextTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        gameState = GameState.IN_PROGRESS;
    }

    public int rollDice() {
        return dice.roll();
    }

    public Map<String, List<Integer>> getCurrentPawnPositions() {
        Map<String, List<Integer>> positions = new HashMap<>();
        for (Player player : players) {
            positions.put(player.getColor(),
                player.getPawns().stream()
                    .map(Pawn::getPosition)
                    .collect(Collectors.toList())
            );
        }
        return positions;
    }

    public void sendGameState(Connection connection) throws IOException {
        GameStateUpdateEvent stateEvent = new GameStateUpdateEvent(
            getCurrentPawnPositions(),
            getCurrentPlayer().getColor(),
            gameState
        );
        connection.send(stateEvent);
    }

    private void initializePlayerPositions() {
        for (Player player : players) {
            player.initializePawns();
        }
    }

    public boolean hasPlayerWon(Player player) {
        return player.getPawnsInHome() == Constants.PAWNS_PER_PLAYER;
    }

    // Getters and utility methods
    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    public Player getPlayerByName(String name) {
        return playerMap.get(name);
    }

    public Board getBoard() {
        return board;
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setGameState(GameState state) {
        this.gameState = state;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public Player getCurrentPlayer() {
        return players.isEmpty() ? null : players.get(currentPlayerIndex);
    }
}
