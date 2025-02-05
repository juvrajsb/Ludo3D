package ludo.core.entities;

import ludo.core.utils.Constants;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Player implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final String color;
    private List<Pawn> pawns;
    private boolean useSingleDie;
    private int startPosition;

    public Player(String name, String color) {
        this.name = name;
        this.color = color;
        this.pawns = new ArrayList<>();
        this.useSingleDie = false;
        initializePawns();
    }

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

    public int getPawnsInHomeColumn(Board board) {//TODO check usage not used currently
        int count = 0;
        for (int i = 0; i < pawns.size(); i++) {
            if (isInHomeColumn(i, board)) {
                count++;
            }
        }
        return count;
    }

    public void setUseSingleDie(boolean useSingleDie) {//TODO check usage not used currently
        this.useSingleDie = useSingleDie;
    }

    public boolean isUseSingleDie() {//TODO check usage not used currently
        return useSingleDie;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public boolean isInHomeColumn(int position) {
        return position >= Constants.BOARD_SIZE - Constants.HOME_COLUMN_SIZE &&
            position < Constants.BOARD_SIZE;
    }

    public void initializePawns() {
        pawns = new ArrayList<>();
        for (int i = 0; i < Constants.PAWNS_PER_PLAYER; i++) {
            pawns.add(new Pawn(color)); // Initialize pawns at the starting position (-1)
        }
    }
}
