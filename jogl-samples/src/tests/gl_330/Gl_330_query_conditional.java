/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests.gl_330;

import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_DYNAMIC_DRAW;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_MAP_INVALIDATE_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_MAP_WRITE_BIT;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL2ES2.GL_ANY_SAMPLES_PASSED;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_QUERY_COUNTER_BITS;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;
import static com.jogamp.opengl.GL2ES3.GL_QUERY_WAIT;
import static com.jogamp.opengl.GL2ES3.GL_UNIFORM_BUFFER;
import static com.jogamp.opengl.GL2ES3.GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import framework.Profile;
import framework.Semantic;
import framework.Test;
import java.nio.ByteBuffer;

/**
 *
 * @author GBarbieri
 */
public class Gl_330_query_conditional extends Test {

    public static void main(String[] args) {
        Gl_330_query_conditional gl_330_query_conditional = new Gl_330_query_conditional();
    }

    public Gl_330_query_conditional() {
        super("gl-330-query-conditional", Profile.CORE, 3, 3);
    }

    private final String SHADERS_SOURCE = "query-conditional";
    private final String SHADERS_ROOT = "src/data/gl_330";

    private int vertexCount = 6;
    private int positionSize = vertexCount * 2 * Float.BYTES;
    private float[] positionData = {
        -1.0f, -1.0f,
        +1.0f, -1.0f,
        +1.0f, 1.0f,
        +1.0f, 1.0f,
        -1.0f, 1.0f,
        -1.0f, -1.0f};

    private enum Buffer {
        VERTEX,
        TRANSFORM,
        MATERIAL,
        MAX
    }

    private int[] vertexArrayName = {0}, queryName = {0}, bufferName = new int[Buffer.MAX.ordinal()];
    private int programName, uniformMaterialOffset;
    private float[] projection = new float[16], model = new float[16];
    private boolean test = true;

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
        if (validated) {
            validated = initQuery(gl3);
        }

        return validated && checkError(gl3, "begin");
    }

    private boolean initQuery(GL3 gl3) {

        gl3.glGenQueries(1, queryName, 0);

        int[] queryBits = {0};
        gl3.glGetQueryiv(GL_ANY_SAMPLES_PASSED, GL_QUERY_COUNTER_BITS, queryBits, 0);

        boolean validated = queryBits[0] >= 1;

        return validated && checkError(gl3, "initQuery");
    }

    private boolean initProgram(GL3 gl3) {

        boolean validated = true;

        if (validated) {

            ShaderProgram shaderProgram = new ShaderProgram();

            ShaderCode vertShaderCode = ShaderCode.create(gl3, GL_VERTEX_SHADER,
                    this.getClass(), SHADERS_ROOT, null, SHADERS_SOURCE, "vert", null, true);
            ShaderCode fragShaderCode = ShaderCode.create(gl3, GL_FRAGMENT_SHADER,
                    this.getClass(), SHADERS_ROOT, null, SHADERS_SOURCE, "frag", null, true);

            shaderProgram.init(gl3);

            shaderProgram.add(vertShaderCode);
            shaderProgram.add(fragShaderCode);

            programName = shaderProgram.program();

            shaderProgram.link(gl3, System.out);
        }

        // Get variables locations
        if (validated) {

            gl3.glUniformBlockBinding(programName, gl3.glGetUniformBlockIndex(programName, "Transform"), Semantic.Uniform.TRANSFORM0);
            gl3.glUniformBlockBinding(programName, gl3.glGetUniformBlockIndex(programName, "Material"), Semantic.Uniform.MATERIAL);
        }

        return validated & checkError(gl3, "initProgram");
    }

    private boolean initBuffer(GL3 gl3) {

        int[] uniformBufferOffset = {0};
        gl3.glGetIntegerv(GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT, uniformBufferOffset, 0);

        gl3.glGenBuffers(Buffer.MAX.ordinal(), bufferName, 0);

        gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.VERTEX.ordinal()]);
        gl3.glBufferData(GL_ARRAY_BUFFER, positionSize, GLBuffers.newDirectFloatBuffer(positionData), GL_STATIC_DRAW);
        gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

        int uniformTransformBlockSize = Math.max(16 * Float.BYTES, uniformBufferOffset[0]);

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.TRANSFORM.ordinal()]);
        gl3.glBufferData(GL_UNIFORM_BUFFER, uniformTransformBlockSize, null, GL_DYNAMIC_DRAW);
        gl3.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        uniformMaterialOffset = Math.max(4 * Float.BYTES, uniformBufferOffset[0]);

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.MATERIAL.ordinal()]);
        gl3.glBufferData(GL_UNIFORM_BUFFER, uniformMaterialOffset * 2, null, GL_STATIC_DRAW);
        {
            ByteBuffer pointer = gl3.glMapBufferRange(GL_UNIFORM_BUFFER, 0, uniformMaterialOffset * 2,
                    GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);

            pointer.putFloat(0.0f);
            pointer.putFloat(0.5f);
            pointer.putFloat(1.0f);
            pointer.putFloat(1.0f);
            pointer.position(uniformMaterialOffset);
            pointer.putFloat(1.0f);
            pointer.putFloat(0.5f);
            pointer.putFloat(0.0f);
            pointer.putFloat(1.0f);
            pointer.rewind();

            gl3.glUnmapBuffer(GL_UNIFORM_BUFFER);
        }
        gl3.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        return checkError(gl3, "initBuffer");
    }

    private boolean initVertexArray(GL3 gl3) {

        gl3.glGenVertexArrays(1, vertexArrayName, 0);
        gl3.glBindVertexArray(vertexArrayName[0]);
        {
            gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.VERTEX.ordinal()]);
            gl3.glVertexAttribPointer(Semantic.Attr.POSITION, 2, GL_FLOAT, false, 0, 0);
            gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

            gl3.glEnableVertexAttribArray(Semantic.Attr.POSITION);
        }
        gl3.glBindVertexArray(0);

        return checkError(gl3, "initVertexArray");
    }

    @Override
    protected boolean render(GL gl) {

        GL3 gl3 = (GL3) gl;

        {
            gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.TRANSFORM.ordinal()]);
            ByteBuffer pointer = gl3.glMapBufferRange(GL_UNIFORM_BUFFER,
                    0, 16 * Float.BYTES, GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);

            FloatUtil.makePerspective(projection, 0, true, (float) Math.PI * 0.25f, (float) windowSize.x / windowSize.y, 0.1f, 100.0f);
            FloatUtil.makeIdentity(model);

            FloatUtil.multMatrix(projection, view());
            FloatUtil.multMatrix(projection, model);

            for (float f : projection) {
                pointer.putFloat(f);
            }
            pointer.rewind();

            gl3.glUnmapBuffer(GL_UNIFORM_BUFFER);
        }

        // Set the display viewport
        gl3.glViewport(0, 0, windowSize.x, windowSize.y);
        gl3.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM0, bufferName[Buffer.TRANSFORM.ordinal()]);

        // Clear color buffer with black
        gl3.glClearBufferfv(GL_COLOR, 0, new float[]{0.0f, 0.0f, 0.0f, 1.0f}, 0);

        // Bind program
        gl3.glUseProgram(programName);
        gl3.glBindVertexArray(vertexArrayName[0]);

        gl3.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, bufferName[Buffer.MATERIAL.ordinal()], 0, 16 * Float.BYTES);

        // The first orange quad is not written in the framebuffer.
        gl3.glColorMaski(0, false, false, false, false);

        // Beginning of the samples count query
        gl3.glBeginQuery(GL_ANY_SAMPLES_PASSED, queryName[0]);
        {
            if (test) {
                // To test the condional rendering, comment this line, the next draw call won't happen.
                gl3.glDrawArraysInstanced(GL_TRIANGLES, 0, vertexCount, 1);
            }
        }
// End of the samples count query
        gl3.glEndQuery(GL_ANY_SAMPLES_PASSED);

        // The second blue quad is written in the framebuffer only if a sample pass the occlusion query.
        gl3.glColorMaski(0, true, true, true, true);

        gl3.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, bufferName[Buffer.MATERIAL.ordinal()],
                uniformMaterialOffset, 4 * Float.BYTES);

        // Draw only if one sample went through the tests, 
        // we don't need to get the query result which prevent the rendering pipeline to stall.
        gl3.glBeginConditionalRender(queryName[0], GL_QUERY_WAIT);
        {
            // Clear color buffer with white
            gl3.glClearBufferfv(GL_COLOR, 0, new float[]{1.0f, 1.0f, 1.0f, 1.0f}, 0);

            gl3.glDrawArraysInstanced(GL_TRIANGLES, 0, vertexCount, 1);
        }
        gl3.glEndConditionalRender();

        test = !test;

        return true;
    }
}