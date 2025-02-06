package ludo.core.game;

import ludo.core.entities.Pawn;
import ludo.core.entities.Player;
import ludo.core.entities.Board;
import ludo.core.entities.Dice;
import ludo.core.utils.Constants;
import ludo.core.network.Connection;
import ludo.core.events.serverToClient.GameStateUpdateEvent;
import ludo.core.validation.MoveValidator;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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

    private GameManager() {
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

    private void validateIndices(int playerIndex, int pawnIndex, int steps) {
        if (playerIndex < 0 || playerIndex >= players.size()) {
            LOGGER.severe("Invalid player index: " + playerIndex);
            throw new IllegalArgumentException("Invalid player index");
        }
        if (pawnIndex < 0 || pawnIndex >= Constants.PAWNS_PER_PLAYER) {
            LOGGER.severe("Invalid pawn index: " + pawnIndex);
            throw new IllegalArgumentException("Invalid pawn index");
        }
        if (steps < 1 || steps > 6) {
            LOGGER.severe("Invalid number of steps: " + steps);
            throw new IllegalArgumentException("Invalid number of steps");
        }
    }

    public boolean canMovePawn(int playerIndex, int pawnIndex, int steps) {
        if (playerIndex < 0 || playerIndex >= players.size() ||
            pawnIndex < 0 || pawnIndex >= Constants.PAWNS_PER_PLAYER) {
            return false;
        }

        Player player = players.get(playerIndex);
        Pawn pawn = player.getPawns().get(pawnIndex);

        // Can only move pawns in home with a 6
        if (pawn.isHome() && steps != 6) {
            return false;
        }

        // If pawn is not in home, check if move is valid
        if (!pawn.isHome()) {
            int newPosition = calculateNewPosition(player, pawn.getPosition(), steps);

            // Check if move would overshoot home column
            if (isHomeColumnOvershoot(player, pawn.getPosition(), newPosition)) {
                return false;
            }

            // Check for collisions with own pawns
            if (!board.isSafeSpot(newPosition)) {
                for (Pawn otherPawn : player.getPawns()) {
                    if (otherPawn != pawn && otherPawn.getPosition() == newPosition) {
                        return false;
                    }
                }
            }
        }

        return true;
    }


//    public synchronized boolean movePawn(String playerId, int pawnIndex, int steps) {
//        Player player = getPlayerByName(playerId);
//        return player != null && movePawn(player, pawnIndex, steps);
//    }
//
//    public synchronized boolean movePawn(Player player, int pawnIndex, int steps) {
//        if (!isValidMove(player, pawnIndex, steps)) {
//            return false;
//        }
//
//        Pawn pawn = player.getPawns().get(pawnIndex);
//        if (!pawn.canMove(steps, board)) {
//            return false;
//        }
//
//        // Execute the move
//        pawn.move(steps, board);
//        checkForCaptures(player, pawn.getPosition());
//        return true;
//    }

    public boolean movePawn(int playerIndex, int pawnIndex, int steps) {
        LOGGER.info(String.format("Attempting move - Player: %d, Pawn: %d, Steps: %d",
            playerIndex, pawnIndex, steps));

        // Validate move
        if (!canMovePawn(playerIndex, pawnIndex, steps)) {
            LOGGER.info("Move validation failed");
            return false;
        }

        Player player = players.get(playerIndex);
        Pawn pawn = player.getPawns().get(pawnIndex);

        // Handle leaving home
        if (pawn.isHome()) {
            if (steps == 6) {
                pawn.leaveHome(board);
                LOGGER.info("Pawn left home to position: " + pawn.getPosition());
                return true;
            }
            return false;
        }

        // Calculate new position
        int newPosition = calculateNewPosition(player, pawn.getPosition(), steps);

        // Execute move
        pawn.setPosition(newPosition);
        LOGGER.info("Moved pawn to position: " + newPosition);

        // Handle captures
        handleCaptures(player, newPosition);

        return true;
    }

    private int calculateNewPosition(Player player, int currentPosition, int steps) {
        if (currentPosition >= board.getTotalSpaces()) {
            return currentPosition + steps;
        }

        int startPos = board.getStartPosition(player.getColor());
        int entryPoint = (startPos + board.getTotalSpaces() - 1) % board.getTotalSpaces();
        int potentialNewPos = currentPosition + steps;

        // Check if entering home column
        if (currentPosition <= entryPoint && potentialNewPos > entryPoint) {
            int stepsAfterEntry = potentialNewPos - entryPoint - 1;
            return board.getTotalSpaces() + (startPos / 13) * board.getHomeColumnSize() + stepsAfterEntry;
        }

        return potentialNewPos % board.getTotalSpaces();
    }

    private boolean isHomeColumnOvershoot(Player player, int currentPos, int newPos) {
        if (currentPos >= board.getTotalSpaces() || newPos >= board.getTotalSpaces()) {
            int homeStart = board.getTotalSpaces() +
                (board.getStartPosition(player.getColor()) / 13) * board.getHomeColumnSize();
            return newPos >= homeStart + board.getHomeColumnSize();
        }
        return false;
    }

    private void handleCaptures(Player movingPlayer, int position) {
        if (board.isSafeSpot(position)) {
            return;
        }

        for (Player otherPlayer : players) {
            if (otherPlayer != movingPlayer) {
                for (Pawn pawn : otherPlayer.getPawns()) {
                    if (pawn.getPosition() == position) {
                        pawn.sendHome();
                        LOGGER.info("Captured " + otherPlayer.getColor() + "'s pawn");
                    }
                }
            }
        }
    }

    public int rollDice() {
        lastDiceRoll = dice.roll();
        return lastDiceRoll;
    }

    public boolean hasValidMovesAvailable(Player player, int diceRoll) {
        int playerIndex = players.indexOf(player);

        // Check each pawn for possible moves
        for (int i = 0; i < player.getPawns().size(); i++) {
            if (canMovePawn(playerIndex, i, diceRoll)) {
                return true;
            }
        }

        return false;
    }

    public void nextTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        lastDiceRoll = 0;
        gameState = GameState.IN_PROGRESS;
        LOGGER.info("this is ok Turn changed to player: " + getCurrentPlayer().getColor());
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
        for (Player player : players) {
            positions.put(player.getColor(),
                player.getPawns().stream()
                    .map(Pawn::getPosition)
                    .collect(Collectors.toList()));
        }
        return positions;
    }

    private void checkForCaptures(Player movingPlayer, int position) { //TODO check usage not used currently
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

    private boolean isValidMove(Player player, int pawnIndex, int steps) { //TODO check usage not used currently
        if (player != getCurrentPlayer() ||
            pawnIndex < 0 ||
            pawnIndex >= Constants.PAWNS_PER_PLAYER) {
            return false;
        }

        Pawn pawn = player.getPawns().get(pawnIndex);
        return MoveValidator.isValidMove(player, pawn, steps, board);
    }

    public void sendGameState(Connection connection) throws IOException {
        GameStateUpdateEvent stateEvent = new GameStateUpdateEvent(
            getCurrentPawnPositions(),
            getCurrentPlayer().getColor(),
            gameState //TODO check; required GameStateType; provided GameState
        );
        connection.send(stateEvent);
    }

    private void initializePlayerPositions() {
        for (Player player : players) {
            player.initializePawns();
        }
    }

    public boolean hasPlayerWon(Player player) { //TODO check usage not used currently
        return player.getPawnsInHome() == Constants.PAWNS_PER_PLAYER;
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

    public GameStateUpdateEvent createGameStateEvent() { //TODO check usage not used currently
        return new GameStateUpdateEvent(
            getCurrentPawnPositions(),
            getCurrentPlayer().getColor(),
            gameState
        );
    }

    public int getPawnPosition(String playerId, int pawnIndex) { //TODO check usage not used currently
        Player player = getPlayerByName(playerId);
        if (player != null && pawnIndex >= 0 && pawnIndex < player.getPawns().size()) {
            return player.getPawns().get(pawnIndex).getPosition();
        }
        return -1;
    }

    public boolean hasPlayerWon(String playerId) {
        Player player = getPlayerByName(playerId);
        return player != null && player.getPawnsInHome() == Constants.PAWNS_PER_PLAYER;
    }

    public boolean canStartGame() {
        return players.size() >= 2 && !gameStarted;
    }
}
