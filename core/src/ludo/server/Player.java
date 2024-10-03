package src.ludo.server;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private String name;
    private String color;
    private List<Pawn> pawns;

    public Player(String name, String color) {
        this.name = name;
        this.color = color;
        this.pawns = new ArrayList<>();
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
}