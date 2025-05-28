package ludo.core.game;

import ludo.core.entities.Pawn;
import ludo.core.entities.Player;
import ludo.core.entities.Board;
import ludo.core.entities.Dice;
import ludo.core.persistence.GamePersistence;
import ludo.core.utils.Constants;
import ludo.core.network.Connection;
import ludo.core.events.serverToClient.GameStateUpdateEvent;
import ludo.core.validation.MoveValidator;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class GameManager {
    private static final Logger LOGGER = Logger.getLogger(GameManager.class.getName());
    private static GameManager instance;
    private final List<Player> players;
    private final Board board;
    private final Dice dice;
    private int currentPlayerIndex;
    private GameState gameState;
    private boolean gameStarted;
    private int lastDiceRoll;
    private final Map<String, Player> playerMap;

    public GameManager() {
        this.players = new CopyOnWriteArrayList<>();
        this.board = new Board();
        this.dice = new Dice();
        this.currentPlayerIndex = 0;
        this.gameState = GameState.WAITING_FOR_PLAYERS;
        this.gameStarted = false;
        this.playerMap = new HashMap<>();
        this.lastDiceRoll = 0;
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

    public synchronized boolean removePlayer(String playerName) {
        return players.removeIf(player -> player.getName().equals(playerName));
    }

    public synchronized void startGame() {
        if (players.size() >= 2) {
            gameStarted = true;
            gameState = GameState.IN_PROGRESS;
            LOGGER.info("Game started, GAMEMANAGER");
            currentPlayerIndex = 0;
            initializePlayerPositions();
        }
    }

    public boolean movePawn(int playerIndex, int pawnIndex, int steps) {
        LOGGER.info(String.format("Move attempt - Player: %d, Pawn: %d, Steps: %d",
            playerIndex, pawnIndex, steps));

        if (playerIndex < 0 || playerIndex >= players.size()) {
            LOGGER.severe("Invalid player index: " + playerIndex);
            return false;
        }

        if (pawnIndex < 0 || pawnIndex >= Constants.PAWNS_PER_PLAYER) {
            LOGGER.severe("Invalid pawn index: " + pawnIndex);
            return false;
        }

        if (steps < 1 || steps > 6) {
            LOGGER.severe("Invalid number of steps: " + steps);
            return false;
        }

        if (steps != lastDiceRoll) {
            LOGGER.severe("Steps don't match last dice roll: " + lastDiceRoll);
            return false;
        }

        Player player = players.get(playerIndex);
        Pawn pawn = player.getPawns().get(pawnIndex);

        if (!MoveValidator.isValidMove(player, pawn, steps, board)) {
            LOGGER.info("Invalid move detected by MoveValidator");
            return false;
        }

        if (pawn.isHome() && steps == 6) {
            int startPosition = board.getStartPositionIndex(player.getColor());
            LOGGER.info("Pawn leaving home. Start position: " + startPosition);

            for (Player otherPlayer : players) {
                if (otherPlayer != player) {
                    for (Pawn otherPawn : otherPlayer.getPawns()) {
                        if (!otherPawn.isHome() && otherPawn.getPosition() == startPosition) {
                            if (!board.isSafeSpot(startPosition)) {
                                // Capture opponent's pawn if not on safe spot
                                otherPawn.sendHome();
                                LOGGER.info("Captured opponent's pawn at start position");
                            }
                        }
                    }
                }
            }

            pawn.setPosition(startPosition);
            pawn.leaveHome(board);
            return true;
        }

        // Calculate and execute move
        int newPosition = calculateNewPosition(player, pawn.getPosition(), steps);
        if (newPosition == -1) {
            LOGGER.info("Invalid move - would overshoot home");
            return false;
        }

        pawn.setPosition(newPosition);
        LOGGER.info("Pawn position updated to: " + newPosition);

        // Handle captures
        handleCaptures(player, newPosition);

        // Check if pawn has reached final position in home column
        if (board.isHomeColumn(newPosition, player.getColor())) {
            int homeStart = board.getTotalSpaces() +
                (board.getStartPositionIndex(player.getColor()) / 13) * Constants.HOME_COLUMN_SIZE;
            int finalHomePosition = homeStart + Constants.HOME_COLUMN_SIZE - 1;

            if (newPosition == finalHomePosition) {
                pawn.setFinished(true);
                LOGGER.info("Pawn has reached final position and is now finished!");
            }
        }

        try {
            GamePersistence.autoSave(players, getCurrentPlayer().getColor(), gameState);
        } catch (Exception e) {
            LOGGER.warning("Failed to auto-save game: " + e.getMessage());
        }

        return true;
    }

    private int calculateNewPosition(Player player, int currentPosition, int steps) {
        if (currentPosition >= board.getTotalSpaces()) {
            // Already in home column
            int newPos = currentPosition + steps;
            if (isHomeColumnOvershoot(player, currentPosition, newPos)) {
                return -1;
            }
            return newPos;
        }

        int startPos = board.getStartPositionIndex(player.getColor());
        int entryPoint = (startPos + board.getTotalSpaces() - 1) % board.getTotalSpaces();
        int potentialNewPos = currentPosition + steps;

        // Check if entering home column
        if (currentPosition <= entryPoint && potentialNewPos > entryPoint) {
            int stepsAfterEntry = potentialNewPos - entryPoint - 1;
            if (stepsAfterEntry >= board.getHomeColumnSize()) {
                return -1;
            }
            return board.getTotalSpaces() + (startPos / 13) * board.getHomeColumnSize() + stepsAfterEntry;
        }

        return potentialNewPos % board.getTotalSpaces();
    }

    private boolean isHomeColumnOvershoot(Player player, int currentPos, int newPos) {
        if (currentPos >= board.getTotalSpaces()) {
            int homeStart = board.getTotalSpaces() +
                (board.getStartPositionIndex(player.getColor()) / 13) * board.getHomeColumnSize();
            return newPos >= homeStart + board.getHomeColumnSize();
        }
        return false;
    }

    public int getLastDiceRoll() {
        return lastDiceRoll;
    }

    public int rollDice() {
        lastDiceRoll = dice.roll();
        return lastDiceRoll;
    }

    public void setLastDiceRoll(int roll) {
        lastDiceRoll = roll;
    }

    public boolean hasValidMovesAvailable(Player player, int diceRoll) {
        LOGGER.info("Checking valid moves for player " + player.getColor() + " with roll " + diceRoll);

        int playerIndex = players.indexOf(player);
        if (playerIndex == -1) {
            LOGGER.warning("Player not found in players list");
            return false;
        }

        // First check if any pawns are at home and can leave with a 6
        boolean hasPawnsAtHome = player.getPawns().stream().anyMatch(Pawn::isHome);
        if (hasPawnsAtHome && diceRoll == 6) {
            // Check if start position is blocked by own pawn
            int startPos = board.getStartPositionIndex(player.getColor());
            boolean startBlocked = player.getPawns().stream()
                .anyMatch(p -> !p.isHome() && p.getPosition() == startPos);

            if (!startBlocked) {
                LOGGER.info("Can move: Pawn can leave home with a 6");
                return true;
            }
        }

        // Then check pawns outside home
        for (int i = 0; i < player.getPawns().size(); i++) {
            Pawn pawn = player.getPawns().get(i);
            if (!pawn.isHome() && !pawn.isFinished()) {
                // Save current position
                int currentPos = pawn.getPosition();

                // Create a copy of the pawn for simulation
                Pawn tempPawn = new Pawn(player.getColor());
                tempPawn.setPosition(currentPos);

                // Check if the move would be valid
                if (MoveValidator.isValidMove(player, tempPawn, diceRoll, board)) {
                    // Simulate the move
                    int newPos = calculateNewPosition(player, currentPos, diceRoll);

                    // Skip if invalid destination
                    if (newPos == -1) continue;

                    // Check collision with own pawns
                    boolean collision = false;
                    if (newPos < board.getTotalSpaces() && !board.isSafeSpot(newPos)) {
                        for (Pawn otherPawn : player.getPawns()) {
                            if (otherPawn != pawn && !otherPawn.isHome() &&
                                otherPawn.getPosition() == newPos) {
                                collision = true;
                                break;
                            }
                        }
                    }

                    if (!collision) {
                        LOGGER.info("Found valid move for pawn " + i + " from " + currentPos + " to " + newPos);
                        return true;
                    }
                }
            }
        }

        LOGGER.info("No valid moves found for player " + player.getColor());
        return false;
    }

    public void nextTurn() {
        if (lastDiceRoll != 6) {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            lastDiceRoll = 0;
            gameState = GameState.IN_PROGRESS;
            LOGGER.info("Turn changed to player: " + getCurrentPlayer().getColor());
        } else {
            LOGGER.info("Player rolled a 6 - keeping turn for: " + getCurrentPlayer().getColor());
        }
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setGameState(GameState state) {
        this.gameState = state;
    }

    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    public Map<String, List<Integer>> getCurrentPawnPositions() {
        Map<String, List<Integer>> positions = new HashMap<>();
        LOGGER.info("Building current pawn positions map");

        for (Player player : players) {
            List<Integer> playerPositions = new ArrayList<>();
            LOGGER.info("Processing player: " + player.getColor());

            for (Pawn pawn : player.getPawns()) {
                int position = pawn.isHome() ? -1 : pawn.getPosition();
                playerPositions.add(position);
            }

            positions.put(player.getColor(), playerPositions);
        }

        LOGGER.info("Complete positions map: " + positions);
        return positions;
    }

    private void handleCaptures(Player movingPlayer, int position) {
        if (board.isSafeSpot(position)) {
            return;
        }

        for (Player otherPlayer : players) {
            if (otherPlayer != movingPlayer) {
                otherPlayer.getPawns().stream()
                    .filter(p -> p.getPosition() == position)
                    .forEach(Pawn::sendHome);
            }
        }
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

    public Player getPlayerByName(String name) {
        return playerMap.get(name);
    }

    public Board getBoard() {
        return board;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public boolean isPlayerTurn(String connectionID) {
        return getCurrentPlayer().getName().equals(connectionID);
    }

    public boolean hasPlayerWon(String playerId) {
        Player player = getPlayerByName(playerId);
        return player != null && player.getPawnsInHome() == Constants.PAWNS_PER_PLAYER;
    }

    public void clearPlayers() {
        players.clear();
        playerMap.clear();
        gameStarted = false;
        currentPlayerIndex = 0;
        lastDiceRoll = 0;
    }
}
