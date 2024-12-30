package ludo.server;

import ludo.Pawn;
import com.badlogic.gdx.graphics.Color;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private String name;
    private String color;
    private List<Pawn> pawns;
    private boolean useSingleDie;
    private ClientHandler clientHandler;
    private int startPosition;
    private int[] pawnPositions;
    private static final int NUM_PLAYERS = 4;
    private static final int PAWNS_PER_PLAYER = 4;
    private static final int BOARD_SIZE = 52;
    private static final int HOME_COLUMN_SIZE = 6;

    public Player(String name, String color) {
        this.name = name;
        this.color = color;
        this.pawns = new ArrayList<>();
        this.useSingleDie = false;
        initializePawns();
    }

//    private void initializePawns() {
//        for (int i = 0; i < PAWNS_PER_PLAYER; i++) {
//            pawns.add(new Pawn(color));
//        }
//    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public List<Pawn> getPawns() {
        return pawns;
    }

    public int getPawnPosition(int pawnIndex) {
        return pawns.get(pawnIndex).getPosition();
    }

    public void setPawnPosition(int pawnIndex, int newPosition) {
        pawns.get(pawnIndex).setPosition(newPosition);
    }

    public int getPawnsInHome() {
        return (int) pawns.stream().filter(Pawn::isFinished).count();
    }

    public boolean isInHomeColumn(int pawnIndex, Board board) {
        return board.isHomeColumn(pawns.get(pawnIndex).getPosition(), color);
    }

    public int getPawnsInHomeColumn(Board board) {
        int count = 0;
        for (int i = 0; i < pawns.size(); i++) {
            if (isInHomeColumn(i, board)) {
                count++;
            }
        }
        return count;
    }

    public void setUseSingleDie(boolean useSingleDie) {
        this.useSingleDie = useSingleDie;
    }

    public boolean isUseSingleDie() {
        return useSingleDie;
    }

    public void setClientHandler(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
    }

    public ClientHandler getClientHandler() {
        return clientHandler;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public boolean isInHomeColumn(int pawnIndex) {
        int position = pawnPositions[pawnIndex];
        return position >= BOARD_SIZE - HOME_COLUMN_SIZE && position < BOARD_SIZE;
    }

    public void initializePawns() {
        pawns = new ArrayList<>();
        for (int i = 0; i < PAWNS_PER_PLAYER; i++) {
            pawns.add(new Pawn(color)); // Initialize pawns at the starting position (-1)
        }
    }
}
//private static class Player {
//    private int id;
//    private int[] pawnPositions;
//    private boolean useSingleDie;
//    private int startPosition;
//
//    public Player(int id) {
//        this.id = id;
//        this.pawnPositions = new int[PAWNS_PER_PLAYER];
//        for (int i = 0; i < PAWNS_PER_PLAYER; i++) {
//            pawnPositions[i] = -1; // -1 indicates the pawn is in the starting area
//        }
//        this.useSingleDie = false;
//        this.startPosition = id * (BOARD_SIZE / NUM_PLAYERS);
//    }
//
//    public Player(String s, String s1) {
//    }
//
//    public int getPawnPosition(int pawnIndex) {
//        return pawnPositions[pawnIndex];
//    }
//
//    public void setPawnPosition(int pawnIndex, int position) {
//        pawnPositions[pawnIndex] = position;
//    }
//
//    public int getPawnsInHome() {
//        int count = 0;
//        for (int position : pawnPositions) {
//            if (position >= BOARD_SIZE) {
//                count++;
//            }
//        }
//        return count;
//    }
//
//    public boolean isInHomeColumn(int pawnIndex) {
//        int position = pawnPositions[pawnIndex];
//        return position >= BOARD_SIZE - HOME_COLUMN_SIZE && position < BOARD_SIZE;
//    }
//
//    public void setUseSingleDie(boolean useSingleDie) {
//        this.useSingleDie = useSingleDie;
//    }
//
//    public boolean isUseSingleDie() {
//        return useSingleDie;
//    }
//
//    public int getStartPosition() {
//        return startPosition;
//    }
//
//    @Override
//    public String toString() {
//        StringBuilder sb = new StringBuilder();
//        sb.append(id).append(":");
//        for (int position : pawnPositions) {
//            sb.append(position).append(",");
//        }
//        sb.append(useSingleDie ? "1" : "0");
//        return sb.toString();
//    }
//}
