package org.devabir.core;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;

public class OpenGlManager {

    // Singleton instance
    private static OpenGlManager instance;
    private boolean isInitialized = false;

    // Private constructor to prevent instantiation
    private OpenGlManager() {
        initGl();
    }

    /**
     * @return create new or return cached GL instance
     */
    public static OpenGlManager getInstance() {
        if (instance == null) {
            instance = new OpenGlManager();
        }
        return instance;
    }

    // Initialize GL
    private void initGl() {
        GL.createCapabilities();
        glEnable(GL_MULTISAMPLE);
        // GL11C.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        glEnable(GL_DEPTH_TEST);
        isInitialized = true;
    }

    /**
     * @return true if GL is initialized
     */
    public boolean isInitialized() {
        return isInitialized;
    }
}
