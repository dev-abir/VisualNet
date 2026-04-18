package org.devabir.common;

import static org.lwjgl.opengl.GL46.*;

/**
 * Vertex Array Object with multiple attributes
 */
public class GLVao {
    private final int id;
    private final String name;

    public GLVao(String name) {
        this.name = name;
        this.id = glGenVertexArrays();
    }

    public void bind() {
        glBindVertexArray(id);
    }

    public void unbind() {
        glBindVertexArray(0);
    }

    /**
     * Attach a buffer as a vertex attribute
     */
    public void attachAttribute(
            GLBuffer buffer,
            int attribLocation,
            int components,
            int type,
            boolean normalized,
            int strideBytes,
            long offsetBytes
    ) {
        bind();
        buffer.bind();

        glVertexAttribPointer(
                attribLocation,
                components,
                type,
                normalized,
                strideBytes,
                offsetBytes
        );

        glEnableVertexAttribArray(attribLocation);

        buffer.unbind();
        unbind();
    }

    public void render(int primitive, int vertexCount) {
        bind();
        glDrawArrays(primitive, 0, vertexCount);
        unbind();
    }

    public void renderInstanced(int primitive, int vertexCount, int nInstances) {
        glDrawArraysInstanced(
                primitive,
                0,          // start at first vertex in VBO
                vertexCount,    // vertices per instance
                nInstances      // number of instances (pixels)
        );
    }

    public void renderElementsInstanced(int primitive, int vertexCount, int nInstances) {
        glDrawElementsInstanced(
                primitive,
                vertexCount,    // number of indices per instance (2 triangles)
                GL_UNSIGNED_INT,// type of the indices
                0,              // offset in the index buffer
                nInstances      // number of instances (pixels)
        );
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void dispose() {
        glDeleteVertexArrays(id);
    }
}
