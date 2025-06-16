package ludo.core.entities;


import java.io.Serializable;
import java.util.logging.Logger;

import static ludo.core.utils.Constants.BOARD_SIZE;
import static ludo.core.utils.Constants.HOME_COLUMN_SIZE;

public class Pawn implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(Pawn.class.getName());
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
        int entryPoint = (startPos + BOARD_SIZE- 1) % BOARD_SIZE;

        if (position <= entryPoint && newPosition > entryPoint) {
            int stepsIntoHome = newPosition - entryPoint;
            return stepsIntoHome <= board.getHomeColumnSize();
        }
        return true;
    }

    public void move(int spaces, Board board) {
        if (!canMove(spaces, board)) {
            return;
        }

        int startPos = board.getStartPosition(color);
        int entryPoint = (startPos + BOARD_SIZE - 1) % BOARD_SIZE;
        int potentialNewPos = position + spaces;

        if (position <= entryPoint && potentialNewPos > entryPoint) {
            int stepsAfterEntry = potentialNewPos - entryPoint;

            if (stepsAfterEntry > board.getHomeColumnSize()) {
                return;
            }
            position = BOARD_SIZE + (startPos / 13) * HOME_COLUMN_SIZE + stepsAfterEntry - 1;
        } else {
            position = potentialNewPos % board.getTotalSpaces();
        }

        // Check if pawn has reached target base
        if (board.isTargetBase(position, color)) {
            isFinished = true;
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
        if (this.isFinished != finished) {
            LOGGER.info("Pawn for " + color + " at position " + position + " isFinished flag set to: " + finished);
        }
        isFinished = finished;
    }
}
