package org.devabir.common;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER;
import static org.lwjgl.opengl.GL43.GL_COMPUTE_SHADER;

public class GlShader {
    private final int programId;

    private GlShader(int programId) {
        this.programId = programId;
    }

    public void use() {
        glUseProgram(programId);
    }

    public int getId() {
        return programId;
    }

    public void dispose() {
        glDeleteProgram(programId);
    }

    // ────────────────────────────────────────────────
    //                BUILDER CLASS
    // ────────────────────────────────────────────────
    public static class Builder {
        private final int programId;

        public Builder() {
            programId = glCreateProgram();
            if (programId == 0) {
                throw new IllegalStateException("Failed to create shader program");
            }
        }

        public Builder attachVertex(String src) {
            attachShader(src, GL_VERTEX_SHADER);
            return this;
        }

        public Builder attachFragment(String src) {
            attachShader(src, GL_FRAGMENT_SHADER);
            return this;
        }

        public Builder attachGeometry(String src) {
            attachShader(src, GL_GEOMETRY_SHADER);
            return this;
        }

        public Builder attachCompute(String src) {
            attachShader(src, GL_COMPUTE_SHADER);
            return this;
        }

        public GlShader link() {
            glLinkProgram(programId);
            if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
                String log = glGetProgramInfoLog(programId);
                throw new RuntimeException("Shader program link failed:\n" + log);
            }
            return new GlShader(programId);
        }

        private void attachShader(String src, int type) {
            int shaderId = glCreateShader(type);
            glShaderSource(shaderId, src);
            glCompileShader(shaderId);

            if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
                String log = glGetShaderInfoLog(shaderId);
                String typeStr;
                switch (type) {
                    case GL_VERTEX_SHADER:
                        typeStr = "Vertex";
                        break;
                    case GL_FRAGMENT_SHADER:
                        typeStr = "Fragment";
                        break;
                    case GL_COMPUTE_SHADER:
                        typeStr = "Compute";
                        break;
                    case GL_GEOMETRY_SHADER:
                        typeStr = "Geometry";
                        break;
                    default:
                        typeStr = "<Unknown type>";
                }
                throw new RuntimeException(typeStr + " shader compile failed:\n" + log);
            }

            glAttachShader(programId, shaderId);
            // We can safely delete it after attachment
            glDeleteShader(shaderId);
        }
    }
}
