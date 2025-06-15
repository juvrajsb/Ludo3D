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
    private boolean pawnWasCaptured = false;
//    private int lastMovePlayerIndex = -1;
//    private int lastMovePawnIndex = -1;
//    private int lastMoveStartPosition = -1;

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
        LOGGER.info(String.format("Move attempt - Player: %d, Pawn: %d, Steps: %d", playerIndex, pawnIndex, steps));

        if (playerIndex < 0 || playerIndex >= players.size()) {
            LOGGER.warning("Invalid player index: " + playerIndex);
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

        // Calculate new position
        int currentPosition = pawn.getPosition();
        int newPosition = calculateNewPosition(player, currentPosition, steps);
        LOGGER.info(String.format("Move validation - Current: %d, New: %d", currentPosition, newPosition));

        // Check if move would overshoot home column
        if (isInHomeColumn(player, currentPosition) && !isInHomeColumn(player, newPosition)) {
            LOGGER.warning("Move invalid - would overshoot home column");
            return false;
        }

        LOGGER.info("Move validation successful");
        pawn.setPosition(newPosition);

        // This calculates the integer position of the final target base
        int homeColumnStartForColor = board.getTotalSpaces() + (board.getStartPositionIndex(player.getColor()) / 13) * (Constants.HOME_COLUMN_SIZE + 1);
        int finalHomePosition = homeColumnStartForColor + Constants.HOME_COLUMN_SIZE;

        // If the pawn has landed on the target base, set its isFinished flag
        if (newPosition == finalHomePosition) {
            pawn.setFinished(true);
            LOGGER.info("Pawn " + pawnIndex + " for player " + player.getColor() + " has finished!");
        }

        // Add capture handling after move
        LOGGER.info("Checking for captures at position: " + newPosition);
        if (!board.isSafeSpot(newPosition)) {
            LOGGER.info("Position " + newPosition + " is not a safe spot - checking for captures");
            handleCaptures(player, newPosition);
        } else {
            LOGGER.info("Position " + newPosition + " is a safe spot - no captures possible");
        }

        return true;
    }

    private int calculateNewPosition(Player player, int currentPosition, int steps) {
        if (currentPosition >= board.getTotalSpaces()) {
            int homeColumnStartForColor = getHomeColumnStartForColor(player.getColor());
            int finalHomePosition = homeColumnStartForColor + Constants.HOME_COLUMN_SIZE;
            if (currentPosition + steps > finalHomePosition) {
                return -1; // Overshot
            }
            return currentPosition + steps;
        }

        int startPos = board.getStartPositionIndex(player.getColor());
        int homeEntryPos = (startPos - 1 + board.getTotalSpaces()) % board.getTotalSpaces();
        int distToEntry = (homeEntryPos - currentPosition + board.getTotalSpaces()) % board.getTotalSpaces();

        if (steps >= distToEntry) {
            int stepsIntoHome = steps - distToEntry;
            if (stepsIntoHome > Constants.HOME_COLUMN_SIZE + 1) {
                return -1; // Overshot.
            }
            int homeColumnStart = getHomeColumnStartForColor(player.getColor());
            return homeColumnStart + stepsIntoHome;
        } else {
            return (currentPosition + steps) % board.getTotalSpaces();
        }
    }

    private int getHomeColumnStartForColor(String color) {
        switch (color.toUpperCase()) {
            case "YELLOW": return 52;
            case "BLUE":   return 58;
            case "RED":    return 64;
            case "GREEN":  return 70;
            default:       return -1;
        }
    }

    private boolean isHomeColumnOvershoot(Player player, int currentPos, int newPos) {
        LOGGER.info("Checking home column overshoot for " + player.getColor() +
            " - Current: " + currentPos + ", New: " + newPos);

        if (currentPos >= board.getTotalSpaces()) {
            int homeStart = board.getTotalSpaces() +
                (board.getStartPositionIndex(player.getColor()) / 13) * board.getHomeColumnSize();
            int homeEnd = homeStart + board.getHomeColumnSize();
            boolean overshoot = newPos >= homeEnd;

            LOGGER.info("Home column boundaries - Start: " + homeStart +
                ", End: " + homeEnd + ", Would overshoot: " + overshoot);
            return overshoot;
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
            // LOGGER.info("Processing player: " + player.getColor());

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
        LOGGER.info("Handling captures for " + movingPlayer.getColor() + " at position " + position);

        if (board.isSafeSpot(position)) {
            LOGGER.info("Position " + position + " is a safe spot - no captures possible");
            return;
        }

        for (Player otherPlayer : players) {
            if (otherPlayer != movingPlayer) {
                LOGGER.info("Checking " + otherPlayer.getColor() + "'s pawns for capture");
                for (Pawn pawn : otherPlayer.getPawns()) {
                    if (!pawn.isHome() && pawn.getPosition() == position) {
                        LOGGER.info("Found " + otherPlayer.getColor() + " pawn at position " + position + " - sending home");
                        pawn.sendHome();
                        pawnWasCaptured = true;
                    }
                }
            }
        }
    }

    public boolean wasPawnCaptured() {
        boolean result = pawnWasCaptured;
        pawnWasCaptured = false;  // Reset the flag after checking
        return result;
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

//    public void undoLastMove() {
//        if (lastMovePlayerIndex != -1 && lastMovePawnIndex != -1) {
//            Player player = players.get(lastMovePlayerIndex);
//            Pawn pawn = player.getPawns().get(lastMovePawnIndex);
//            pawn.setPosition(lastMoveStartPosition);
//
//            // Reset move tracking
//            lastMovePlayerIndex = -1;
//            lastMovePawnIndex = -1;
//            lastMoveStartPosition = -1;
//        }
//    }

    private void checkForCaptures(Player player, int position) {
        LOGGER.info(String.format("Checking for captures at position %d", position));
        for (Player otherPlayer : players) {
            if (otherPlayer != player) {
                for (Pawn otherPawn : otherPlayer.getPawns()) {
                    if (!otherPawn.isHome() && otherPawn.getPosition() == position) {
                        if (!board.isSafeSpot(position)) {
                            LOGGER.info(String.format("Capturing %s pawn at position %d",
                                otherPlayer.getColor(), position));
                            otherPawn.sendHome();
                        }
                    }
                }
            }
        }
    }

    private boolean isInHomeColumn(Player player, int position) {
        return board.isHomeColumn(position, player.getColor());
    }
}
