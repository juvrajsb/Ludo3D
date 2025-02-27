package ludo.core.entities;

public class Dice {
    private int currentValue;

    public Dice() {
        this.currentValue = 1;
    }

    public int roll() {
        this.currentValue = (int)(Math.random() * 6) + 1;
        return this.currentValue;
    }

//    public int getCurrentValue() {//TODO check usage not used currently
//        return this.currentValue;
//    }
}
