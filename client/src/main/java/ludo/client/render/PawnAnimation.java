package ludo.client.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;

public class PawnAnimation {
    private final Vector3 startPos;
    private final Vector3 targetPos;
    private final Vector3 controlPoint;
    private float progress;
    private static final float ANIMATION_SPEED = 2.0f;
    private static final float ARC_HEIGHT = 0.8f;

    PawnAnimation(Vector3 start, Vector3 target, String color) {
        this.startPos = GameRenderer.applyOffset(start.cpy());
        this.targetPos = GameRenderer.applyOffset(target.cpy());

        this.controlPoint = new Vector3(
            (start.x + target.x) * 0.5f,
            start.y + ARC_HEIGHT,
            (start.z + target.z) * 0.5f
        );
        this.progress = 0;

        Gdx.app.log("PawnAnimation", String.format(
            "Creating animation for %s pawn: Start(%s) -> Target(%s)",
            color, start.toString(), target.toString()));
    }

    Vector3 getCurrentPosition() {
        float t = progress;
        Vector3 currentPos = new Vector3();

        // Quadratic Bezier curve for smooth arc movement
        float oneMinusT = 1 - t;
        currentPos.x = oneMinusT * oneMinusT * startPos.x + 2 * oneMinusT * t * controlPoint.x + t * t * targetPos.x;
        currentPos.y = oneMinusT * oneMinusT * startPos.y + 2 * oneMinusT * t * controlPoint.y + t * t * targetPos.y;
        currentPos.z = oneMinusT * oneMinusT * startPos.z + 2 * oneMinusT * t * controlPoint.z + t * t * targetPos.z;

        return currentPos;
    }

    boolean update(float deltaTime) {
        progress = Math.min(1.0f, progress + deltaTime * ANIMATION_SPEED);
        // Gdx.app.log("PawnAnimation", String.format(
        //     "Updating animation progress: %f, Current position: %s",
        //     progress, getCurrentPosition().toString()));
        return progress >= 1.0f;
    }
}

