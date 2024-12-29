package ludo.server;

import ludo.Pawn;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LudoGame {
    private static final int NUM_PLAYERS = 4;
    private static final int PAWNS_PER_PLAYER = 4;
    private static final int BOARD_SIZE = 52;
    private static final int HOME_COLUMN_SIZE = 6;

    private List<Player> players;
    private int currentPlayerIndex;
    private Random random;
    private int[] diceRolls;
    private Board board;

    public LudoGame() {
        random = new Random();
        diceRolls = new int[2];
        board = new Board();
    }

    public void initializeGame(List<Player> players) {
        this.players = new ArrayList<>(players);
        currentPlayerIndex = 0;

        // Initialize pawns for each player
        for (Player player : this.players) {
            player.initializePawns();
        }
    }

    public int rollDice() {
        return random.nextInt(6) + 1;
    }

    public boolean movePawn(int playerIndex, int pawnIndex, int steps) {
        // Basic validation
        if (playerIndex < 0 || playerIndex >= NUM_PLAYERS) {
            throw new IllegalArgumentException("Invalid player index");
        }
        if (pawnIndex < 0 || pawnIndex >= PAWNS_PER_PLAYER) {
            throw new IllegalArgumentException("Invalid pawn index");
        }
        if (steps < 1 || steps > 6) {
            throw new IllegalArgumentException("Invalid number of steps");
        }

        Player player = players.get(playerIndex);
        Pawn pawn = player.getPawns().get(pawnIndex);
        int currentPosition = pawn.getPosition();

        // Rule: Can only leave home with a 6
        if (currentPosition == -1 && steps != 6) {
            return false;
        }

        // If pawn is in home and rolled a 6, place it on start position
        if (currentPosition == -1 && steps == 6) {
            pawn.leaveHome(board);
            return true;
        }

        // Calculate new position
        int newPosition = calculateNewPosition(player, currentPosition, steps);

        // Invalid move if calculateNewPosition returns -1
        if (newPosition == -1) {
            return false;
        }

        // Check for collisions with own pawns
        for (int i = 0; i < PAWNS_PER_PLAYER; i++) {
            if (i != pawnIndex && player.getPawns().get(i).getPosition() == newPosition
                && !board.isSafeSpot(newPosition % BOARD_SIZE)) {
                return false;
            }
        }

        // Handle captures on main board only
        if (newPosition < BOARD_SIZE && !board.isSafeSpot(newPosition)) {
            for (Player opponent : players) {
                if (opponent != player) {
                    for (Pawn opponentPawn : opponent.getPawns()) {
                        if (opponentPawn.getPosition() == newPosition) {
                            opponentPawn.sendHome();
                            break;
                        }
                    }
                }
            }
        }

        pawn.setPosition(newPosition);
        return true;
    }

    private int calculateNewPosition(Player player, int currentPosition, int steps) {
        // If pawn is in home (-1), and we get a 6, move to start position
        if (currentPosition == -1) {
            if (steps == 6) {
                return player.getStartPosition();
            }
            return -1;
        }

        // If pawn is already in home column
        if (currentPosition >= BOARD_SIZE) {
            int homePosition = currentPosition + steps;
            if (homePosition >= BOARD_SIZE + HOME_COLUMN_SIZE) {
                return -1; // Would overshoot home
            }
            return homePosition;
        }

        int playerStart = player.getStartPosition();
        int entryPoint = (playerStart + BOARD_SIZE - 1) % BOARD_SIZE;

        // First, check if we're going to enter home column
        if (currentPosition <= entryPoint &&
            (currentPosition + steps) > entryPoint &&
            playerStart == board.getStartPosition(player.getColor())) {

            int stepsAfterEntry = (currentPosition + steps) - entryPoint - 1;
            if (stepsAfterEntry < HOME_COLUMN_SIZE) {
                // Calculate home column position based on player's start position
                int homeColumnBase = BOARD_SIZE + (playerStart / 13) * HOME_COLUMN_SIZE;
                return homeColumnBase + stepsAfterEntry;
            }
            return -1; // Would overshoot home
        }

        // Regular movement with wraparound
        int newPosition = currentPosition + steps;
        if (newPosition >= BOARD_SIZE) {
            newPosition = newPosition % BOARD_SIZE;
        }
        return newPosition;
    }

      public String getGameState() {
        StringBuilder state = new StringBuilder();
        for (Player player : players) {
            state.append(player.toString()).append(";");
        }
        return state.toString();
    }

    public void nextTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % NUM_PLAYERS;
    }

    public boolean isGameOver() {
        for (Player player : players) {
            if (player.getPawnsInHome() == PAWNS_PER_PLAYER) {
                return true;
            }
        }
        return false;
    }

    public void updateGameState(String stateString) {
        String[] playerStates = stateString.split(";");
        for (int i = 0; i < playerStates.length; i++) {
            String[] playerData = playerStates[i].split(":");
            int playerId = Integer.parseInt(playerData[0]);
            String[] pawnPositions = playerData[1].split(",");
            Player player = players.get(playerId);
            for (int j = 0; j < pawnPositions.length; j++) {
                int position = Integer.parseInt(pawnPositions[j]);
                player.setPawnPosition(j, position);
            }
            player.setUseSingleDie(playerData[2].equals("1"));
        }
    }

    public List<Player> getPlayers() {
        return players;
    }

    public int[] getDiceRolls() {
        return diceRolls;
    }

    public boolean canMovePawn(int playerIndex, int pawnIndex, int steps) {
        if (playerIndex < 0 || playerIndex >= NUM_PLAYERS ||
            pawnIndex < 0 || pawnIndex >= PAWNS_PER_PLAYER ||
            steps < 1 || steps > 6) {
            return false;
        }

        Player player = players.get(playerIndex);
        int currentPosition = player.getPawnPosition(pawnIndex);

        // If pawn is in home, can only move with a 6
        if (currentPosition == -1) {
            return steps == 6;
        }

        int playerStart = player.getStartPosition();
        int newPosition = currentPosition + steps;
        int entryPoint = (playerStart - 1 + BOARD_SIZE) % BOARD_SIZE;

        // Check if pawn will enter home column
        if (currentPosition <= entryPoint && newPosition > entryPoint) {
            int stepsIntoHome = newPosition - entryPoint - 1;
            if (stepsIntoHome >= HOME_COLUMN_SIZE) {
                return false; // Would overshoot home
            }

            // Check if new home column position is blocked by own pawn
            int homePosition = BOARD_SIZE + stepsIntoHome;
            for (Pawn p : player.getPawns()) {
                if (p.getPosition() == homePosition) {
                    return false;
                }
            }
            return true;
        }

        // Normal board movement
        newPosition = newPosition % BOARD_SIZE;

        // Check if destination is occupied by own pawn
        for (Pawn p : player.getPawns()) {
            if (p.getPosition() == newPosition && !board.isSafeSpot(newPosition)) {
                return false;
            }
        }

        return true;
    }

    public boolean makeMove(Player player, String move) {
        try {
            String[] parts = move.split(" ");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid move format");
            }
            int pawnIndex = Integer.parseInt(parts[0]);
            int steps = Integer.parseInt(parts[1]);

            int playerIndex = players.indexOf(player);
            if (playerIndex == -1) {
                throw new IllegalArgumentException("Player not found in the game");
            }

            return movePawn(playerIndex, pawnIndex, steps);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid pawn index or steps");
        }
    }

    public Player getWinner() {
        for (Player player : players) {
            if (player.getPawnsInHome() == PAWNS_PER_PLAYER) {
                return player;
            }
        }
        return null;
    }

    public String getSerializedGameState() {
        StringBuilder sb = new StringBuilder();
        for (Player player : players) {
            sb.append(player.getColor()).append(":");
            for (Pawn pawn : player.getPawns()) {
                sb.append(pawn.getPosition()).append(",");
            }
            sb.setLength(sb.length() - 1); // Remove last comma
            sb.append(";");
        }
        sb.setLength(sb.length() - 1); // Remove last semicolon
        return sb.toString();
    }

    public Board getBoard() {
        return board;
    }
}
