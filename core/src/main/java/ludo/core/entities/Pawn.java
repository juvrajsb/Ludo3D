package ludo.core.entities;


import java.io.Serializable;

import static ludo.core.utils.Constants.BOARD_SIZE;
import static ludo.core.utils.Constants.HOME_COLUMN_SIZE;

public class Pawn implements Serializable {
    private String color;
    private int position;
    private boolean isHome;
    private boolean isFinished;

    public Pawn(String color) {
        this.color = color;
        this.position = -1;
        this.isHome = true;
        this.isFinished = false;
    }

    public boolean canMove(int spaces, Board board) {
        // Can't move if at home or finished
        if (isHome || isFinished) {
            return false;
        }

        int newPosition = (position + spaces) % board.getTotalSpaces();

        // Check home column entry
        if (board.isHomeColumn(newPosition, color)) {
            // Check if would overshoot home
            return newPosition < board.getStartPosition(color) + board.getTotalSpaces() - 1;
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
//    public boolean movePawn(int playerIndex, int pawnIndex, int steps) {
//        validateIndices(playerIndex, pawnIndex, steps);
//
//        Player player = players.get(playerIndex);
//        Pawn pawn = player.getPawns().get(pawnIndex);
//        // Handle pawn in home
//        if (pawn.isHome()) {
//            if (steps == 6) {
//                pawn.leaveHome(board);
//                return true;
//            }
//            return false;
//        }
//
//        int currentPosition = pawn.getPosition();
//        int startPos = board.getStartPosition(player.getColor());
//        int entryPoint = (startPos + BOARD_SIZE - 1) % BOARD_SIZE;
//        int potentialNewPos = currentPosition + steps;
//
//        // Calculate new position
//        int newPosition;
//        if (currentPosition <= entryPoint && potentialNewPos > entryPoint) {
//            // Entering home column
//            int stepsAfterEntry = potentialNewPos - entryPoint - 1;
//
//            if (stepsAfterEntry >= HOME_COLUMN_SIZE) {
//                return false; // Would overshoot home
//            }
//
//            newPosition = BOARD_SIZE + (startPos / 13) * HOME_COLUMN_SIZE + stepsAfterEntry;
//        } else {
//            // Regular movement
//            newPosition = potentialNewPos % BOARD_SIZE;
//        }
//
//        // Check for collisions with own pawns
//        if (!board.isSafeSpot(newPosition)) {
//            for (Pawn otherPawn : player.getPawns()) {
//                if (otherPawn != pawn && otherPawn.getPosition() == newPosition) {
//                    return false;
//                }
//            }
//        }
//
//        // Execute move
//        pawn.setPosition(newPosition);
//        handleCaptures(player, newPosition);
//        return true;
//    }

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
