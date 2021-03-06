/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests.gl_410;

import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL2ES3.*;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import glm.glm;
import glm.mat._4.Mat4;
import glm.vec._3.Vec3;
import framework.BufferUtils;
import framework.Profile;
import framework.Semantic;
import framework.Test;
import glm.vec._2.Vec2;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 *
 * @author GBarbieri
 */
public class Gl_410_buffer_uniform_array extends Test {

    public static void main(String[] args) {
        Gl_410_buffer_uniform_array gl_410_buffer_uniform_array = new Gl_410_buffer_uniform_array();
    }

    public Gl_410_buffer_uniform_array() {
        super("gl-410-buffer-uniform-array", Profile.CORE, 4, 1);
    }

    private final String SHADERS_SOURCE = "buffer-uniform-array";
    private final String SHADERS_ROOT = "src/data/gl_410";

    private int vertexCount = 4;
    private int positionSize = vertexCount * Vec2.SIZE;
    private Vec2[] positionData = {
        new Vec2(-1.0f, -1.0f).mul(0.8f),
        new Vec2(+1.0f, -1.0f).mul(0.8f),
        new Vec2(+1.0f, +1.0f).mul(0.8f),
        new Vec2(-1.0f, +1.0f).mul(0.8f)};

    private int elementCount = 6;
    private int elementSize = elementCount * Short.BYTES;
    private short[] elementData = {
        0, 1, 2,
        2, 3, 0};

    private class Buffer {

        public static final int VERTEX = 0;
        public static final int INSTANCE = 1;
        public static final int ELEMENT = 2;
        public static final int TRANSFORM = 3;
        public static final int MATERIAL = 4;
        public static final int MAX = 5;
    }

    private IntBuffer vertexArrayName = GLBuffers.newDirectIntBuffer(1),
            bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX),
            uniformBufferAlignment = GLBuffers.newDirectIntBuffer(1);
    private int programName;

    @Override
    protected boolean begin(GL gl) {

        GL4 gl4 = (GL4) gl;

        boolean validated = true;

        if (validated) {
            validated = initProgram(gl4);
        }
        if (validated) {
            validated = initBuffer(gl4);
        }
        if (validated) {
            validated = initVertexArray(gl4);
        }

        gl4.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        gl4.glDrawBuffer(GL_BACK);
        if (!isFramebufferComplete(gl, 0)) {
            return false;
        }

        return validated;
    }

    private boolean initProgram(GL4 gl4) {

        boolean validated = true;

        // Create program
        if (validated) {

            ShaderProgram shaderProgram = new ShaderProgram();

            ShaderCode vertexShaderCode = ShaderCode.create(gl4, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT,
                    null, SHADERS_SOURCE, "vert", null, true);
            ShaderCode fragmentShaderCode = ShaderCode.create(gl4, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT,
                    null, SHADERS_SOURCE, "frag", null, true);

            shaderProgram.init(gl4);

            shaderProgram.add(vertexShaderCode);
            shaderProgram.add(fragmentShaderCode);

            programName = shaderProgram.program();

            shaderProgram.link(gl4, System.out);

        }

        // Get variables locations
        if (validated) {

            int uniformMaterial = gl4.glGetUniformBlockIndex(programName, "Material");
            int uniformTransform0 = gl4.glGetUniformBlockIndex(programName, "Transform[0]");
            int uniformTransform1 = gl4.glGetUniformBlockIndex(programName, "Transform[1]");

            gl4.glUniformBlockBinding(programName, uniformMaterial, Semantic.Uniform.MATERIAL);
            gl4.glUniformBlockBinding(programName, uniformTransform0, Semantic.Uniform.TRANSFORM0);
            gl4.glUniformBlockBinding(programName, uniformTransform1, Semantic.Uniform.TRANSFORM1);
        }

        return validated & checkError(gl4, "initProgram");
    }

    private boolean initVertexArray(GL4 gl4) {

        gl4.glGenVertexArrays(1, vertexArrayName);
        gl4.glBindVertexArray(vertexArrayName.get(0));
        {
            gl4.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
            gl4.glVertexAttribPointer(Semantic.Attr.POSITION, 2, GL_FLOAT, false, 0, 0);
            gl4.glEnableVertexAttribArray(Semantic.Attr.POSITION);

            gl4.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.INSTANCE));
            gl4.glVertexAttribIPointer(Semantic.Attr.DRAW_ID, 1, GL_INT, 0, 0);
            gl4.glVertexAttribDivisor(Semantic.Attr.DRAW_ID, 1);
            gl4.glEnableVertexAttribArray(Semantic.Attr.DRAW_ID);

            gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        }
        gl4.glBindVertexArray(0);

        return true;
    }

    private boolean initBuffer(GL4 gl4) {

        ShortBuffer elementBuffer = GLBuffers.newDirectShortBuffer(elementData);
        ByteBuffer positionBuffer = GLBuffers.newDirectByteBuffer(positionSize);
        IntBuffer instanceBuffer = GLBuffers.newDirectIntBuffer(new int[]{0, 1});
        FloatBuffer diffuseBuffer = GLBuffers.newDirectFloatBuffer(
                new float[]{1.0f, 0.5f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f, 1.0f});

        gl4.glGetIntegerv(GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT, uniformBufferAlignment);

        gl4.glGenBuffers(Buffer.MAX, bufferName);

        gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        gl4.glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementSize, elementBuffer, GL_STATIC_DRAW);
        gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        gl4.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
        for (int i = 0; i < vertexCount; i++) {
            positionData[i].toDbb(positionBuffer, i * Vec2.SIZE);
        }
        gl4.glBufferData(GL_ARRAY_BUFFER, positionSize, positionBuffer, GL_STATIC_DRAW);
        gl4.glBindBuffer(GL_ARRAY_BUFFER, 0);

        gl4.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.INSTANCE));
        gl4.glBufferData(GL_ARRAY_BUFFER, instanceBuffer.capacity() * Integer.BYTES, instanceBuffer, GL_STATIC_DRAW);
        gl4.glBindBuffer(GL_ARRAY_BUFFER, 0);

        int uniformBufferSize = Math.max(uniformBufferAlignment.get(0), Mat4.SIZE) * 2;

        {
            gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM));
            gl4.glBufferData(GL_UNIFORM_BUFFER, uniformBufferSize, null, GL_DYNAMIC_DRAW);
            gl4.glBindBuffer(GL_UNIFORM_BUFFER, 0);
        }

        {

            gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.MATERIAL));
            gl4.glBufferData(GL_UNIFORM_BUFFER, diffuseBuffer.capacity() * Float.BYTES, diffuseBuffer, GL_STATIC_DRAW);
            gl4.glBindBuffer(GL_UNIFORM_BUFFER, 0);
        }

        BufferUtils.destroyDirectBuffer(elementBuffer);
        BufferUtils.destroyDirectBuffer(positionBuffer);
        BufferUtils.destroyDirectBuffer(instanceBuffer);
        BufferUtils.destroyDirectBuffer(diffuseBuffer);

        return true;
    }

    @Override
    protected boolean render(GL gl) {

        GL4 gl4 = (GL4) gl;

        int uniformBufferOffset = Math.max(uniformBufferAlignment.get(0), Mat4.SIZE);
        int uniformBufferRange = uniformBufferOffset * 2;

        {
            gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM));
            ByteBuffer pointer = gl4.glMapBufferRange(GL_UNIFORM_BUFFER, 0,
                    uniformBufferRange, GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);

            Mat4 projection = glm.perspective_((float) Math.PI * 0.25f, 4.0f / 3.0f, 0.1f, 100.0f);
            Mat4 model0 = new Mat4(1.0f).translate(new Vec3(+1, 0, 0));
            Mat4 model1 = new Mat4(1.0f).translate(new Vec3(-1, 0, 0));

            pointer.position(uniformBufferOffset * 0);
            pointer.asFloatBuffer().put(projection.mul_(viewMat4()).mul(model0).toFa_());
            pointer.position(uniformBufferOffset * 1);
            pointer.asFloatBuffer().put(projection.mul_(viewMat4()).mul(model1).toFa_());
            pointer.rewind();

            // Make sure the uniform buffer is uploaded
            gl4.glUnmapBuffer(GL_UNIFORM_BUFFER);
            gl4.glBindBuffer(GL_UNIFORM_BUFFER, 0);
        }

        gl4.glViewport(0, 0, windowSize.x, windowSize.y);
        gl4.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 1).put(1, 1).put(2, 1).put(3, 1));

        gl4.glUseProgram(programName);

        // Attach the buffer to UBO binding point semantic::uniform::MATERIAL
        gl4.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, bufferName.get(Buffer.MATERIAL));
        gl4.glBindVertexArray(vertexArrayName.get(0));

        // Attach the buffer to UBO binding point semantic::uniform::TRANSFORM0
        gl4.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM0, bufferName.get(Buffer.TRANSFORM), 0,
                Mat4.SIZE);
        gl4.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM1, bufferName.get(Buffer.TRANSFORM),
                uniformBufferOffset, Mat4.SIZE);

        gl4.glDrawElementsInstancedBaseVertexBaseInstance(GL_TRIANGLES, elementCount, GL_UNSIGNED_SHORT, 0, 1, 0, 0);
        gl4.glDrawElementsInstancedBaseVertexBaseInstance(GL_TRIANGLES, elementCount, GL_UNSIGNED_SHORT, 0, 1, 0, 1);

        return true;
    }

    @Override
    protected boolean end(GL gl) {

        GL4 gl4 = (GL4) gl;

        gl4.glDeleteVertexArrays(1, vertexArrayName);
        gl4.glDeleteBuffers(Buffer.MAX, bufferName);
        gl4.glDeleteProgram(programName);

        BufferUtils.destroyDirectBuffer(vertexArrayName);
        BufferUtils.destroyDirectBuffer(bufferName);
        BufferUtils.destroyDirectBuffer(uniformBufferAlignment);

        return true;
    }
}
