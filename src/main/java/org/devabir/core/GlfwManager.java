package org.devabir.core;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glViewport;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

/**
 * Singleton class to manage GLFW
 */
public class GlfwManager {

    private static final Logger log = Logger.getLogger(GlfwManager.class.getName());

    // Singleton instance
    private static GlfwManager instance;
    // Pointer to the GLFW window
    private static long windowPtr;
    // Window props
    private final int windowWidth;
    private final int windowHeight;
    private boolean isInitialized = false;

    // Private constructor to prevent instantiation
    private GlfwManager(int windowWidth, int windowHeight) {
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        initGLFW();
    }

    /**
     * @return create new or return cached GLFW instance
     */
    public static GlfwManager getInstance(int windowWidth, int windowHeight) {
        if (instance == null) {
            instance = new GlfwManager(windowWidth, windowHeight);
        }
        return instance;
    }

    // Initialize GLFW
    private void initGLFW() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Set up a window
        glfwDefaultWindowHints();
        // glfwWindowHint(GLFW_REFRESH_RATE, 120);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_SAMPLES, 4);
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        windowPtr = glfwCreateWindow(windowWidth, windowHeight, "VisualNet", MemoryUtil.NULL, MemoryUtil.NULL);
        if (windowPtr == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create window");
        }

        glfwMakeContextCurrent(windowPtr);
        glfwSwapInterval(1);

        ByteBuffer buffer = BufferUtils.createByteBuffer(16 * 16 * 4);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {

                boolean insideTriangle = x <= y;

                if (insideTriangle) {
                    // White pixel
                    buffer.put((byte) 255); // R
                    buffer.put((byte) 255); // G
                    buffer.put((byte) 255); // B
                    buffer.put((byte) 255); // A
                } else {
                    // Transparent
                    buffer.put((byte) 0); // R
                    buffer.put((byte) 0); // G
                    buffer.put((byte) 0); // B
                    buffer.put((byte) 0); // A
                }
            }
        }
        buffer.flip();

        GLFWImage image = GLFWImage.malloc();
        image.set(16,16, buffer);

        GLFWImage.Buffer images = GLFWImage.malloc(1);
        images.put(0, image);

        glfwSetWindowIcon(windowPtr, images);
        images.free();
        image.free();

        glfwSetFramebufferSizeCallback(windowPtr, (win, w, h) -> glViewport(0, 0, w, h));

        glfwShowWindow(windowPtr);

        isInitialized = true;
        log.info("GLFW initialized");
    }

    public boolean isKeyPressed(int glfwKey) {
        return glfwGetKey(windowPtr, glfwKey) == GLFW_PRESS;
    }

    public boolean isKeyReleased(int glfwKey) {
        return glfwGetKey(windowPtr, glfwKey) == GLFW_RELEASE;
    }

    /**
     * Cleanup method to terminate GLFW
     */
    public void dispose() {
        if (isInitialized) {
            glfwDestroyWindow(windowPtr);
            glfwTerminate();
            log.info("GLFW Terminated");
            isInitialized = false;
        }
    }

    /**
     * @return the window pointer, after initialization
     */
    public long getWindowPtr() {
        return windowPtr;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }
}
