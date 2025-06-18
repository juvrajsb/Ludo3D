// In ludo/client/render/Skybox.java
package ludo.client.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;

public class Skybox implements Disposable {

    private final Mesh cubeMesh;
    private final ShaderProgram shader;
    private final Cubemap cubemap;
    private final Matrix4 worldTransform = new Matrix4();

    public Skybox() {
        cubemap = new Cubemap(
            Gdx.files.internal("images/background_right.jpg"),
            Gdx.files.internal("images/background_left.jpg"),
            Gdx.files.internal("images/background_up.jpg"),
            Gdx.files.internal("images/background_down.jpg"),
            Gdx.files.internal("images/background_front.jpg"),
            Gdx.files.internal("images/background_back.jpg")
        );

        String vertexShader =
            "attribute vec3 a_position;\n" +
                "varying vec3 v_texCoords;\n" +
                "uniform mat4 u_projViewTrans;\n" +
                "void main() {\n" +
                "  v_texCoords = a_position;\n" +
                "  gl_Position = (u_projViewTrans * vec4(a_position, 1.0)).xyww;\n" +
                "}";

        String fragmentShader =
            "#ifdef GL_ES\n" +
                "precision mediump float;\n" +
                "#endif\n" +
                "varying vec3 v_texCoords;\n" +
                "uniform samplerCube u_environmentCubemap;\n" +
                "void main() {\n" +
                "  gl_FragColor = textureCube(u_environmentCubemap, v_texCoords);\n" +
                "}";

        shader = new ShaderProgram(vertexShader, fragmentShader);
        if (!shader.isCompiled()) {
            throw new IllegalArgumentException("Error compiling shader: " + shader.getLog());
        }

        cubeMesh = createCubeMesh();
    }

    private Mesh createCubeMesh() {
        float[] vertices = {
            -1f, -1f, -1f,  1f, -1f, -1f,  1f,  1f, -1f, -1f,  1f, -1f,
            -1f, -1f,  1f,  1f, -1f,  1f,  1f,  1f,  1f, -1f,  1f,  1f,
        };

        short[] indices = {
            0, 1, 2, 0, 2, 3, // Back
            4, 5, 6, 4, 6, 7, // Front
            0, 4, 7, 0, 7, 3, // Left
            1, 5, 6, 1, 6, 2, // Right
            3, 2, 6, 3, 6, 7, // Top
            0, 1, 5, 0, 5, 4  // Bottom
        };

        Mesh mesh = new Mesh(true, 8, 36, new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"));
        mesh.setVertices(vertices);
        mesh.setIndices(indices);
        return mesh;
    }

    public void render(Camera camera) {
        worldTransform.set(camera.view);
        worldTransform.setTranslation(0, 0, 0);
        worldTransform.mulLeft(camera.projection);

        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);

        shader.bind();
        shader.setUniformMatrix("u_projViewTrans", worldTransform);

        cubemap.bind(0);
        shader.setUniformi("u_environmentCubemap", 0);

        cubeMesh.render(shader, GL20.GL_TRIANGLES);

        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
    }

    @Override
    public void dispose() {
        cubeMesh.dispose();
        shader.dispose();
        cubemap.dispose();
    }
}
