package ludo.core.entities;


public class Pawn {
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

        position = (position + spaces) % board.getTotalSpaces();

        // Check if pawn finished
        if (board.isHomeColumn(position, color)) {
            if (position == board.getStartPosition(color) + board.getTotalSpaces() - 1) {
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
