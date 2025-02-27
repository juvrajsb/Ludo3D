package ludo.client.render;

import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import ludo.client.assets.GameAssets;

public class DiceRenderer {
    private static final float ROLL_DURATION = 1.0f;
    private static final float DICE_SCALE = 40f;
    private final ModelInstance diceInstance;
    private final Vector3 position;
    private final Vector3 rotation;
    private final Quaternion quaternion;
    private final Matrix4[] valueFaceRotations = new Matrix4[6];
    private final Quaternion tempRotation = new Quaternion();
    private float animationTime;
    private boolean isRolling;
    private int currentValue;
    private int targetValue;

    public DiceRenderer() {
        Model diceModel = GameAssets.getInstance().getDiceModel();
        diceInstance = new ModelInstance(diceModel);
        position = new Vector3(0, 1f, 0); // Position the dice on the board
        rotation = new Vector3();
        quaternion = new Quaternion();
        initializeFaceRotations();

        // Set initial material for the dice
        Material material = new Material(ColorAttribute.createDiffuse(1f, 1f, 1f, 1f));
        diceInstance.materials.get(0).set(material);

        reset();
        updateTransform();
    }

    private void initializeFaceRotations() {
        valueFaceRotations[0] = new Matrix4().setToRotation(Vector3.Z, -90); // 1
        valueFaceRotations[1] = new Matrix4().setToRotation(Vector3.X, 180); // 2
        valueFaceRotations[2] = new Matrix4().setToRotation(Vector3.X, 90);  // 3
        valueFaceRotations[3] = new Matrix4().setToRotation(Vector3.X, -90); // 4
        valueFaceRotations[4] = new Matrix4().setToRotation(Vector3.X, 0); // 5
        valueFaceRotations[5] = new Matrix4().setToRotation(Vector3.Z, 90); // 6
    }

    public void update(float deltaTime) {
        if (isRolling) {
            animationTime += deltaTime;

            if (animationTime >= ROLL_DURATION) {
                isRolling = false;
                currentValue = targetValue;
                setFaceRotation(currentValue);
            } else {
                // During animation
                float progress = animationTime / ROLL_DURATION;

                // Random rotation during roll
                rotation.add(
                    (float)(Math.random() * 30),
                    (float)(Math.random() * 30),
                    (float)(Math.random() * 30)
                );

                // Interpolate to target rotation near end of animation
                if (progress > 0.8f) {
                    float t = (progress - 0.8f) / 0.2f;
                    interpolateToTargetRotation(t);
                }

                updateTransform();
            }
        }
    }

    public void startRoll(int newValue) {
        if (newValue < 1 || newValue > 6) return;

        isRolling = true;
        animationTime = 0;
        targetValue = newValue;

        rotation.set(
            (float)(Math.random() * 360),
            (float)(Math.random() * 360),
            (float)(Math.random() * 360)
        );
    }

    private void interpolateToTargetRotation(float t) {
        Matrix4 targetRotation = valueFaceRotations[targetValue - 1];
        tempRotation.setFromMatrix(targetRotation);
        quaternion.slerp(tempRotation, t);
    }

    private void setFaceRotation(int value) {
        if (value < 1 || value > 6) return;
        diceInstance.transform.set(valueFaceRotations[value - 1]);
        updateTransform();
    }

    private void updateTransform() {
        if (diceInstance != null) {
            diceInstance.transform.setToTranslation(position);
            diceInstance.transform.rotate(quaternion);
            diceInstance.transform.scale(DICE_SCALE, DICE_SCALE, DICE_SCALE);
        }
    }

    public void render(ModelBatch modelBatch) {
        if (diceInstance != null) {
            modelBatch.render(diceInstance);
        }
    }

    public void reset() {
        isRolling = false;
        animationTime = 0;
        currentValue = 1;
        rotation.set(0, 0, 0);
        quaternion.idt();
        setFaceRotation(currentValue);
    }

//    public boolean isRolling() {
//        return isRolling;
//    }//TODO check usage not used currently
//
//    public int getCurrentValue() {
//        return currentValue;
//    }//TODO check usage not used currently

    public void dispose() {
        // Model disposal is handled by GameAssets
    }
}
