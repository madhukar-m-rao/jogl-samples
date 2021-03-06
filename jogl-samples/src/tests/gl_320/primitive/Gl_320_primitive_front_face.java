/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests.gl_320.primitive;

import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL2ES3.*;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import framework.BufferUtils;
import glm.glm;
import glm.mat._4.Mat4;
import framework.Profile;
import framework.Semantic;
import framework.Test;
import glm.vec._2.Vec2;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 *
 * @author GBarbieri
 */
public class Gl_320_primitive_front_face extends Test {

    public static void main(String[] args) {
        Gl_320_primitive_front_face gl_320_primitive_front_face = new Gl_320_primitive_front_face();
    }

    public Gl_320_primitive_front_face() {
        super("gl-320-primitive-front-face", Profile.CORE, 3, 2);
    }

    private final String SHADERS_SOURCE = "primitive-front-face";
    private final String SHADERS_ROOT = "src/data/gl_320/primitive";

    private int vertexCount = 4;
    private int vertexSize = vertexCount * Vec2.SIZE;
    private float[] vertexData = {
        -1.0f, -1.0f,
        +1.0f, -1.0f,
        +1.0f, +1.0f,
        -1.0f, +1.0f};

    private int elementCount = 6;
    private int elementSize = elementCount * Short.BYTES;
    private short[] elementData = {
        0, 1, 2,
        2, 3, 0};

    private class Buffer {

        public static final int ELEMENT = 0;
        public static final int VERTEX = 1;
        public static final int MAX = 2;
    };

    private int programName, uniformMvp;
    private IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX),
            vertexArrayName = GLBuffers.newDirectIntBuffer(1);
    private FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer(new float[]{0.0f, 0.0f, 0.0f, 1.0f});

    @Override
    protected boolean begin(GL gl) {

        GL3 gl3 = (GL3) gl;

        boolean validated = true;

        if (validated) {
            validated = initProgram(gl3);
        }
        if (validated) {
            validated = initBuffer(gl3);
        }
        if (validated) {
            validated = initVertexArray(gl3);
        }

        return validated && checkError(gl3, "begin");
    }

    private boolean initProgram(GL3 gl3) {

        boolean validated = true;

        if (validated) {

            ShaderCode vertShaderCode = ShaderCode.create(gl3, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_SOURCE, "vert", null, true);
            ShaderCode fragShaderCode = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_SOURCE, "frag", null, true);

            ShaderProgram shaderProgram = new ShaderProgram();
            shaderProgram.add(vertShaderCode);
            shaderProgram.add(fragShaderCode);

            shaderProgram.init(gl3);

            programName = shaderProgram.program();

            gl3.glBindAttribLocation(programName, Semantic.Attr.POSITION, "position");
            gl3.glBindFragDataLocation(programName, Semantic.Frag.COLOR, "color");

            shaderProgram.link(gl3, System.out);
        }
        if (validated) {

            uniformMvp = gl3.glGetUniformLocation(programName, "mvp");
        }

        return validated & checkError(gl3, "initProgram");
    }

    private boolean initVertexArray(GL3 gl3) {
        // Build a vertex array object
        gl3.glGenVertexArrays(1, vertexArrayName);
        gl3.glBindVertexArray(vertexArrayName.get(0));
        {
            gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
            gl3.glVertexAttribPointer(Semantic.Attr.POSITION, 2, GL_FLOAT, false, Vec2.SIZE, 0);

            gl3.glEnableVertexAttribArray(Semantic.Attr.POSITION);
            gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

            gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        }
        gl3.glBindVertexArray(0);

        return checkError(gl3, "initVertexArray");
    }

    private boolean initBuffer(GL3 gl3) {

        ShortBuffer elementBuffer = GLBuffers.newDirectShortBuffer(elementData);
        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData);

        // Generate a buffer object
        gl3.glGenBuffers(Buffer.MAX, bufferName);

        gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        gl3.glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementSize, elementBuffer, GL_STATIC_DRAW);
        gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
        gl3.glBufferData(GL_ARRAY_BUFFER, vertexSize, vertexBuffer, GL_STATIC_DRAW);
        gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

        BufferUtils.destroyDirectBuffer(elementBuffer);
        BufferUtils.destroyDirectBuffer(vertexBuffer);

        return checkError(gl3, "initBuffer");
    }

    @Override
    protected boolean render(GL gl) {

        GL3 gl3 = (GL3) gl;

        Mat4 projection = glm.perspective_((float) Math.PI * 0.25f, (float) windowSize.x / windowSize.y, 0.1f, 100.0f);
        Mat4 model = new Mat4(1.0f);
        Mat4 mvp = projection.mul(viewMat4()).mul(model);

        // Clear color buffer with black
        gl3.glClearBufferfv(GL_COLOR, 0, clearColor);

        // Bind program
        gl3.glUseProgram(programName);
        // Set the value of MVP uniform.
        gl3.glUniformMatrix4fv(uniformMvp, 1, false, mvp.toFa_(), 0);

        // Bind vertex array & draw 
        gl3.glBindVertexArray(vertexArrayName.get(0));

        // Set the display viewport
        gl3.glViewport(0, 0, windowSize.x, windowSize.y);

        gl3.glProvokingVertex(GL_LAST_VERTEX_CONVENTION);
        gl3.glDrawElementsInstancedBaseVertex(GL_TRIANGLES, elementCount, GL_UNSIGNED_SHORT, 0, 1, 0);

        return true;
    }

    @Override
    protected boolean end(GL gl) {

        GL3 gl3 = (GL3) gl;

        gl3.glDeleteVertexArrays(1, vertexArrayName);
        gl3.glDeleteBuffers(Buffer.MAX, bufferName);
        gl3.glDeleteProgram(programName);

        BufferUtils.destroyDirectBuffer(vertexArrayName);
        BufferUtils.destroyDirectBuffer(bufferName);

        BufferUtils.destroyDirectBuffer(clearColor);

        return checkError(gl3, "end");
    }
}
