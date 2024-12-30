package ludo;

import com.badlogic.gdx.graphics.Color;
import ludo.server.Board;

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

    public void move(int spaces, Board board) {
        if (!isHome && !isFinished) {
            position = (position + spaces) % board.getTotalSpaces();
            if (board.isHomeColumn(position, color)) {
                if (position == board.getStartPosition(color) + board.getTotalSpaces() - 1) {
                    isFinished = true;
                }
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
