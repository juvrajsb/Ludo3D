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
        // Can only leave home with a 6
        if (isHome && spaces != 6) {
            return false;
        }

        // Cannot move if finished
        if (isFinished) {
            return false;
        }

        // If in home, any 6 is valid
        if (isHome && spaces == 6) {
            return true;
        }

        int newPosition = position + spaces;
        
        // Check if entering home column
        int startPos = board.getStartPosition(color);
        int entryPoint = (startPos + board.getTotalSpaces() - 1) % board.getTotalSpaces();
        
        if (position <= entryPoint && newPosition > entryPoint) {
            int stepsIntoHome = newPosition - entryPoint - 1;
            // Check if would overshoot home column
            return stepsIntoHome < board.getHomeColumnSize();
        }

        // Regular movement is always valid if not entering home column
        return true;
    }

    public void move(int spaces, Board board) {
        if (!canMove(spaces, board)) {
            return;
        }

        int startPos = board.getStartPosition(color);
        int entryPoint = (startPos + board.getTotalSpaces() - 1) % board.getTotalSpaces();
        int potentialNewPos = position + spaces;

        // Calculate new position
        if (position <= entryPoint && potentialNewPos > entryPoint) {
            // Entering home column
            int stepsAfterEntry = potentialNewPos - entryPoint - 1;

            // Check if would overshoot home
            if (stepsAfterEntry >= board.getHomeColumnSize()) {
                return;
            }

            // Calculate home column position
            position = board.getTotalSpaces() + (startPos / 13) * board.getHomeColumnSize() + stepsAfterEntry;
        } else {
            // Regular board movement
            position = potentialNewPos % board.getTotalSpaces();
        }

        // Check if pawn finished
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
