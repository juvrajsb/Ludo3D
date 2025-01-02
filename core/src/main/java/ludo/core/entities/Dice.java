package ludo.core.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.utils.UBJsonReader;

/**
 * Represents a dice in the Ludo game
 */
public class Dice {
    private Model diceModel;
    private int currentValue;

    public Dice() {
        this.diceModel = new G3dModelLoader(new UBJsonReader()).loadModel(Gdx.files.internal("models/dice.g3db"));
        this.currentValue = 1;
    }

    public int roll() {
        this.currentValue = (int)(Math.random() * 6) + 1;
        return this.currentValue;
    }

    public int getCurrentValue() {
        return this.currentValue;
    }

    public Model getModel() {
        return this.diceModel;
    }
}
