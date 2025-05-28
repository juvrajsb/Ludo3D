package ludo.core.entities;


import java.io.Serializable;

import static ludo.core.utils.Constants.BOARD_SIZE;
import static ludo.core.utils.Constants.HOME_COLUMN_SIZE;

public class Pawn implements Serializable {
    private final String color;
    private int position;
    private boolean isHome;
    private boolean isFinished;

    public Pawn(String color) {
        this.color = color.toUpperCase();
        this.position = -1;
        this.isHome = true;
        this.isFinished = false;
    }

    public boolean canMove(int spaces, Board board) {
        if (isHome && spaces != 6) {
            return false;
        }

        if (isFinished) {
            return false;
        }

        if (isHome && spaces == 6) {
            return true;
        }

        int newPosition = position + spaces;

        int startPos = board.getStartPosition(color);
        int entryPoint = (startPos + board.getTotalSpaces() - 1) % board.getTotalSpaces();

        if (position <= entryPoint && newPosition > entryPoint) {
            int stepsIntoHome = newPosition - entryPoint - 1;
            return stepsIntoHome < board.getHomeColumnSize();
        }
        return true;
    }

    public void move(int spaces, Board board) {
        if (!canMove(spaces, board)) {
            return;
        }

        int startPos = board.getStartPosition(color);
        int entryPoint = (startPos + board.getTotalSpaces() - 1) % board.getTotalSpaces();
        int potentialNewPos = position + spaces;

        if (position <= entryPoint && potentialNewPos > entryPoint) {
            int stepsAfterEntry = potentialNewPos - entryPoint - 1;

            if (stepsAfterEntry >= board.getHomeColumnSize()) {
                return;
            }
            position = board.getTotalSpaces() + (startPos / 13) * board.getHomeColumnSize() + stepsAfterEntry;
        } else {
            position = potentialNewPos % board.getTotalSpaces();
        }

        if (board.isHomeColumn(position, color)) {
            int finalHomePosition = board.getTotalSpaces() + (startPos / 13) * board.getHomeColumnSize() + (board.getHomeColumnSize() - 1);
            if (position == finalHomePosition) {
                isFinished = true;
            }
        }
    }

    public void sendHome() {
        isHome = true;
        position = -1;
    }

    public void leaveHome(Board board) {
        if (isHome) {
            isHome = false;
            position = board.getStartPosition(color);
        }
    }

    public String getColor() {
        return color;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
        this.isHome = position == -1;
    }

    public boolean isHome() {
        return isHome;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }
}
