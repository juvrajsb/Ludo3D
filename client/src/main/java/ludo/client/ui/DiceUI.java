package ludo.client.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;

/**
 * UI component that displays a dice face
 */
public class DiceUI extends Stack {
    private Image[] diceFaces;
    private int currentFace;

    public DiceUI() {
        // Load the 6 dice face textures
        diceFaces = new Image[6];
        for (int i = 0; i < 6; i++) {
            diceFaces[i] = new Image(new Texture("dice" + (i+1) + ".png"));
            addActor(diceFaces[i]);
            diceFaces[i].setVisible(false);
        }

        // Show first face by default
        currentFace = 0;
        diceFaces[currentFace].setVisible(true);
    }

    /**
     * Shows the specified dice face (1-6)
     * @param value the dice value to display
     */
    public void showFace(int value) {
        // Hide current face
        diceFaces[currentFace].setVisible(false);

        // Show new face (subtract 1 since array is 0-based)
        currentFace = value - 1;
        diceFaces[currentFace].setVisible(true);
    }

    @Override
    public void dispose() {
        // Clean up textures
        for (Image face : diceFaces) {
            face.getDrawable().getRegion().getTexture().dispose();
        }
    }
}
