package src.ludo.server;

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

    public LudoGame() {
        players = new ArrayList<>();
        random = new Random();
        diceRolls = new int[2];
    }

    public void initializeGame() {
        String[] colour = {"red", "green", "blue", "yellow"};
        players.clear();
        for (int i = 0; i < NUM_PLAYERS; i++) {
            players.add(new Player("Player " + (i + 1), colour[i]));
        }
        currentPlayerIndex = 0;
    }

    public int[] rollDice() {
        diceRolls[0] = random.nextInt(6) + 1;
        diceRolls[1] = random.nextInt(6) + 1;
        return diceRolls;
    }

    public boolean movePawn(int playerIndex, int pawnIndex, int steps) {
        if (playerIndex < 0 || playerIndex >= NUM_PLAYERS) {
            throw new IllegalArgumentException("Invalid player index");
        }
        if (pawnIndex < 0 || pawnIndex >= PAWNS_PER_PLAYER) {
            throw new IllegalArgumentException("Invalid pawn index");
        }
        if (steps < 1 || steps > 12) {
            throw new IllegalArgumentException("Invalid number of steps");
        }

        Player player = players.get(playerIndex);
        int currentPosition = player.getPawnPosition(pawnIndex);

        // Error handling
        if (playerIndex < 0 || playerIndex >= NUM_PLAYERS) {
            throw new IllegalArgumentException("Invalid player index");
        }
        if (pawnIndex < 0 || pawnIndex >= PAWNS_PER_PLAYER) {
            throw new IllegalArgumentException("Invalid pawn index");
        }
        if (steps < 1 || steps > 12) {
            throw new IllegalArgumentException("Invalid number of steps");
        }

        // Rule 10: If no pieces on board, can only enter with a double
        if (currentPosition == -1 && diceRolls[0] != diceRolls[1]) {
            return false;
        }

        int newPosition = calculateNewPosition(currentPosition, steps, player.getStartPosition());

        // Check if the pawn is entering the home column
        if (newPosition >= BOARD_SIZE && currentPosition < BOARD_SIZE) {
            if (newPosition > BOARD_SIZE + HOME_COLUMN_SIZE - 1) {
                return false; // Overshot the home
            }
            // Entering home column
            player.setPawnPosition(pawnIndex, newPosition);
            return true;
        }

        // Rule 5: Pieces can only move forward
        if (newPosition < currentPosition && currentPosition != -1) {
            return false;
        }

        // Rule 7: Check if the new position is occupied by own piece
        for (int i = 0; i < PAWNS_PER_PLAYER; i++) {
            if (i != pawnIndex && player.getPawnPosition(i) == newPosition) {
                return false;
            }
        }

        // Rule 6: Knock off opponent's piece
        for (Player opponent : players) {
            if (opponent != player) {
                for (int i = 0; i < PAWNS_PER_PLAYER; i++) {
                    if (opponent.getPawnPosition(i) == newPosition) {
                        opponent.setPawnPosition(i, -1);
                        break;
                    }
                }
            }
        }

        player.setPawnPosition(pawnIndex, newPosition);

        // Rule 9: Check if player should switch to one die
        if (player.getPawnsInHome() == 3 && player.isInHomeColumn(pawnIndex)) {
            player.setUseSingleDie(true);
        }

        return true;
    }

    private int calculateNewPosition(int currentPosition, int steps, int startPosition) {
        if (currentPosition == -1) {
            return startPosition;
        }
        return (currentPosition + steps) % BOARD_SIZE;
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
        Player player = players.get(playerIndex);
        int currentPosition = player.getPawnPosition(pawnIndex);
        int newPosition = calculateNewPosition(currentPosition, steps, player.getStartPosition());

        // Implement logic to check if the move is valid
        // This should include checks for:
        // - Moving out of the starting area
        // - Not overshooting the home column
        // - Not landing on your own pawn
        // - Any other game-specific rules

        return true; // Placeholder, replace with actual logic
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

    private static class Player {
        private int id;
        private int[] pawnPositions;
        private boolean useSingleDie;
        private int startPosition;

        public Player(int id) {
            this.id = id;
            this.pawnPositions = new int[PAWNS_PER_PLAYER];
            for (int i = 0; i < PAWNS_PER_PLAYER; i++) {
                pawnPositions[i] = -1; // -1 indicates the pawn is in the starting area
            }
            this.useSingleDie = false;
            this.startPosition = id * (BOARD_SIZE / NUM_PLAYERS);
        }

        public int getPawnPosition(int pawnIndex) {
            return pawnPositions[pawnIndex];
        }

        public void setPawnPosition(int pawnIndex, int position) {
            pawnPositions[pawnIndex] = position;
        }

        public int getPawnsInHome() {
            int count = 0;
            for (int position : pawnPositions) {
                if (position >= BOARD_SIZE) {
                    count++;
                }
            }
            return count;
        }

        public boolean isInHomeColumn(int pawnIndex) {
            int position = pawnPositions[pawnIndex];
            return position >= BOARD_SIZE - HOME_COLUMN_SIZE && position < BOARD_SIZE;
        }

        public void setUseSingleDie(boolean useSingleDie) {
            this.useSingleDie = useSingleDie;
        }

        public boolean isUseSingleDie() {
            return useSingleDie;
        }

        public int getStartPosition() {
            return startPosition;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(id).append(":");
            for (int position : pawnPositions) {
                sb.append(position).append(",");
            }
            sb.append(useSingleDie ? "1" : "0");
            return sb.toString();
        }
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
}