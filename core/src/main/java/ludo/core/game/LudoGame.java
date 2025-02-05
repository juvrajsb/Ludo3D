package ludo.core.game;

import ludo.core.entities.Board;
import ludo.core.entities.Pawn;
import ludo.core.entities.Player;
import ludo.core.utils.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static ludo.core.utils.Constants.*;
//for testing
public class LudoGame {
    private final List<Player> players;
    private final Board board;
    private final Random random;
    private int currentPlayerIndex;

    public LudoGame() {
        this.players = new ArrayList<>();
        this.board = new Board();
        this.random = new Random();
        this.currentPlayerIndex = 0;
    }

    public void initializeGame(List<Player> players) {
        if (players.size() > MAX_PLAYERS) {
            throw new IllegalArgumentException("Too many players");
        }
        this.players.clear();
        this.players.addAll(players);

        // Initialize pawns for each player
        for (Player player : this.players) {
            player.initializePawns();
        }
    }

    public int rollDice() {
        return random.nextInt(6) + 1;
    }

    public boolean movePawn(int playerIndex, int pawnIndex, int steps) {
        validateIndices(playerIndex, pawnIndex, steps);

        Player player = players.get(playerIndex);
        Pawn pawn = player.getPawns().get(pawnIndex);
        // Handle pawn in home
        if (pawn.isHome()) {
            if (steps == 6) {
                pawn.leaveHome(board);
                return true;
            }
            return false;
        }

        int currentPosition = pawn.getPosition();
        int startPos = board.getStartPosition(player.getColor());
        int entryPoint = (startPos + BOARD_SIZE - 1) % BOARD_SIZE;
        int potentialNewPos = currentPosition + steps;

        // Calculate new position
        int newPosition;
        if (currentPosition <= entryPoint && potentialNewPos > entryPoint) {
            // Entering home column
            int stepsAfterEntry = potentialNewPos - entryPoint - 1;

            if (stepsAfterEntry >= HOME_COLUMN_SIZE) {
                return false; // Would overshoot home
            }

            newPosition = BOARD_SIZE + (startPos / 13) * HOME_COLUMN_SIZE + stepsAfterEntry;
        } else {
            // Regular movement
            newPosition = potentialNewPos % BOARD_SIZE;
        }

        // Check for collisions with own pawns
        if (!board.isSafeSpot(newPosition)) {
            for (Pawn otherPawn : player.getPawns()) {
                if (otherPawn != pawn && otherPawn.getPosition() == newPosition) {
                    return false;
                }
            }
        }

        // Execute move
        pawn.setPosition(newPosition);
        handleCaptures(player, newPosition);
        return true;
    }

    private void validateIndices(int playerIndex, int pawnIndex, int steps) {
        if (playerIndex < 0 || playerIndex >= players.size()) {
            throw new IllegalArgumentException("Invalid player index");
        }
        if (pawnIndex < 0 || pawnIndex >= PAWNS_PER_PLAYER) {
            throw new IllegalArgumentException("Invalid pawn index");
        }
        if (steps < 1 || steps > 6) {
            throw new IllegalArgumentException("Invalid number of steps");
        }
    }

    private boolean isValidMove(Player player, Pawn pawn, int steps) {
        // Can only leave home with a 6
        if (pawn.isHome() && steps != 6) {
            return false;
        }

        // If pawn is in home column, check for overshooting
        if (player.isInHomeColumn(pawn.getPosition())) {
            int newPosition = pawn.getPosition() + steps;
            return newPosition < BOARD_SIZE + HOME_COLUMN_SIZE;
        }

        // Check for collisions with own pawns on regular spaces
        int newPosition = calculateNewPosition(player, pawn.getPosition(), steps);
        if (newPosition < BOARD_SIZE && !board.isSafeSpot(newPosition)) {
            for (Pawn otherPawn : player.getPawns()) {
                if (otherPawn != pawn && otherPawn.getPosition() == newPosition) {
                    return false;
                }
            }
        }

        return true;
    }

    private void executeMove(Player player, Pawn pawn, int steps) {
        if (pawn.isHome()) {
            pawn.leaveHome(board);
            return;
        }

        int newPosition = calculateNewPosition(player, pawn.getPosition(), steps);

        // Handle captures
        if (newPosition < BOARD_SIZE && !board.isSafeSpot(newPosition)) {
            handleCaptures(player, newPosition);
        }

        pawn.setPosition(newPosition);
    }

    private void handleCaptures(Player movingPlayer, int position) {
        for (Player player : players) {
            if (player != movingPlayer) {
                for (Pawn pawn : player.getPawns()) {
                    if (pawn.getPosition() == position) {
                        pawn.sendHome();
                    }
                }
            }
        }
    }

    private int calculateNewPosition(Player player, int currentPosition, int steps) {
        // If in home column
        if (currentPosition >= BOARD_SIZE) {
            return currentPosition + steps;
        }

        int playerStart = board.getStartPosition(player.getColor());
        int entryPoint = (playerStart + BOARD_SIZE - 1) % BOARD_SIZE;
        int newPosition = currentPosition + steps;

        // Check if entering home column
        if (currentPosition <= entryPoint && newPosition > entryPoint) {
            int stepsAfterEntry = newPosition - entryPoint - 1;
            if (stepsAfterEntry < Constants.HOME_COLUMN_SIZE) {
                return BOARD_SIZE + (playerStart / 13) * Constants.HOME_COLUMN_SIZE + stepsAfterEntry;
            }
            return -1; // Invalid move - would overshoot home
        }

        // Regular board movement with wraparound
        return newPosition % BOARD_SIZE;
    }

    public void nextTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    public boolean isGameOver() {
        return players.stream().anyMatch(p -> p.getPawnsInHome() == Constants.PAWNS_PER_PLAYER);
    }

    public Player getWinner() {
        return players.stream()
            .filter(p -> p.getPawnsInHome() == Constants.PAWNS_PER_PLAYER)
            .findFirst()
            .orElse(null);
    }

    public String getGameState() {
        StringBuilder state = new StringBuilder();
        for (Player player : players) {
            state.append(player.getColor()).append(":");
            for (Pawn pawn : player.getPawns()) {
                state.append(pawn.getPosition()).append(",");
            }
            state.setLength(state.length() - 1); // Remove last comma
            state.append(";");
        }
        if (state.length() > 0) {
            state.setLength(state.length() - 1); // Remove last semicolon
        }
        return state.toString();
    }

    public void updateGameState(String stateString) {
        String[] playerStates = stateString.split(";");
        for (String playerState : playerStates) {
            String[] parts = playerState.split(":");
            String color = parts[0];
            String[] positions = parts[1].split(",");

            Player player = findPlayerByColor(color);
            if (player != null) {
                for (int i = 0; i < positions.length; i++) {
                    player.setPawnPosition(i, Integer.parseInt(positions[i]));
                }
            }
        }
    }

    private Player findPlayerByColor(String color) {
        return players.stream()
            .filter(p -> p.getColor().equals(color))
            .findFirst()
            .orElse(null);
    }

    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public Board getBoard() {
        return board;
    }

    public boolean canMovePawn(int playerIndex, int pawnIndex, int steps) {
        try {
            validateIndices(playerIndex, pawnIndex, steps);
            Player player = players.get(playerIndex);
            Pawn pawn = player.getPawns().get(pawnIndex);
            return isValidMove(player, pawn, steps);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
