package ludo.server.game;

import ludo.core.entities.Player;
import ludo.core.entities.Board;
import ludo.core.entities.Dice;
import ludo.core.utils.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameManager {
    private static GameManager instance;
    private final List<Player> players;
    private final Board board;
    private final Dice dice;
    private int currentPlayerIndex;
    private GameState gameState;
    private boolean gameStarted;

    private GameManager() {
        this.players = new CopyOnWriteArrayList<>();
        this.board = new Board();
        this.dice = new Dice();
        this.currentPlayerIndex = 0;
        this.gameState = GameState.WAITING_FOR_PLAYERS;
        this.gameStarted = false;
    }

    public static synchronized GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    public boolean addPlayer(Player player) {
        if (players.size() >= Constants.MAX_PLAYERS || gameStarted) {
            return false;
        }
        return players.add(player);
    }

    public void startGame() {
        if (players.size() >= 2) {
            gameStarted = true;
            gameState = GameState.IN_PROGRESS;
            currentPlayerIndex = 0;
            initializePlayerPositions();
        }
    }

    private void initializePlayerPositions() {
        for (Player player : players) {
            player.initializePawns();
        }
    }

    public boolean movePawn(Player player, int pawnIndex, int steps) {
        if (!isValidMove(player, pawnIndex, steps)) {
            return false;
        }

        // Execute the move
        player.getPawns().get(pawnIndex).move(steps, board);

        // Check if this player has won
        if (hasPlayerWon(player)) {
            gameState = GameState.GAME_OVER;
        }

        return true;
    }

    private boolean isValidMove(Player player, int pawnIndex, int steps) {
        if (player != getCurrentPlayer() || pawnIndex < 0 || pawnIndex >= Constants.PAWNS_PER_PLAYER) {
            return false;
        }

        // Add more move validation logic here
        return true;
    }

    public void nextTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public int rollDice() {
        return dice.roll();
    }

    public boolean hasPlayerWon(Player player) {
        return player.getPawnsInHome() == Constants.PAWNS_PER_PLAYER;
    }

    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    public Board getBoard() {
        return board;
    }

    public GameState getGameState() {
        return gameState;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }
}
