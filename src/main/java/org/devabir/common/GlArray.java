package org.devabir.common;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL46.*;

/**
 * Utility class for GL array buffer manipulation
 */
public class GlArray {
    private final String name;
    private final int vboId;
    private final int vaoId;
    private final int vertexCount;

    /**
     * @param name                name for the buffer allocated
     * @param vertices            Array contents.
     * @param componentsPerVertex components per vertex (2D / 3D - like 2 for x and y, then 3 for x, y and z
     * @param glUsage             usage GL_STATIC_DRAW, GL_DYNAMIC_DRAW, etc.
     * @param shaderLoc           layout location referred in the shader
     */
    public GlArray(String name, float[] vertices, int componentsPerVertex, int glUsage, int shaderLoc) {
        this.name = name;
        if (vertices.length % componentsPerVertex != 0)
            throw new IllegalArgumentException("vertices.length is not a multiple of componentsPerVertex.");
        this.vertexCount = vertices.length / componentsPerVertex;

        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);

        // Allocate CPU memory (RAM)
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();

        // Transfer to VRAM
        glBufferData(GL_ARRAY_BUFFER, buffer, glUsage);

        // layout(location = 0) in vec2 position; or vec3 position
        glVertexAttribPointer(shaderLoc, componentsPerVertex, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        // Free
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public int getVboId() {
        return vboId;
    }

    public int getVaoId() {
        return vaoId;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public String getName() {
        return name;
    }

    public void bind() {
        glBindVertexArray(vaoId);
    }

    public void unbind() {
        glBindVertexArray(0);
    }

    public void render(int glPrimitive) {
        bind();
        glDrawArrays(glPrimitive, 0, vertexCount);
        unbind();
    }

    public void dispose() {
        glDeleteBuffers(vboId);
        glDeleteVertexArrays(vaoId);
    }

}
