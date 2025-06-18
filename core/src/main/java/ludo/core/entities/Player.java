package ludo.core.entities;

import ludo.core.utils.Constants;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Player implements Serializable {
    private final String name;
    private final String color;
    private List<Pawn> pawns;
    private boolean isDisconnected = false;

    public Player(String name, String color) {
        this.name = name;
        this.color = color.toUpperCase();
        this.pawns = new ArrayList<>();
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

    public int getPawnsInHome() {
        return (int) pawns.stream().filter(Pawn::isFinished).count();
    }

    public void initializePawns() {
        pawns = new ArrayList<>();
        for (int i = 0; i < Constants.PAWNS_PER_PLAYER; i++) {
            pawns.add(new Pawn(color));
        }
    }

    public boolean isDisconnected() {
        return isDisconnected;
    }

    public void setDisconnected(boolean disconnected) {
        isDisconnected = disconnected;
    }
}
