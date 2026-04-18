package org.devabir.core;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWNativeGLX;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.glfw.GLFWNativeX11;
import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CL10GL;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.windows.User32;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.logging.Logger;

import static org.lwjgl.glfw.GLFWNativeWGL.glfwGetWGLContext;
import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.opencl.KHRGLSharing.*;

public class OpenClManager {

    private static final Logger log = Logger.getLogger(OpenClManager.class.getName());

    // Singleton instance
    private static OpenClManager instance;
    private static GlfwManager glfwManager;
    private boolean isInitialized = false;
    private long clQueuePtr;
    private long clContextPtr;
    private long clPlatform;
    private long clDevice;

    // Private constructor to prevent instantiation
    private OpenClManager() {
        initCl();
    }

    /**
     * @return create new or return cached GL instance
     */
    public static OpenClManager getInstance(GlfwManager glfwManager) {
        if (OpenClManager.glfwManager != null)
            throw new IllegalStateException("Init called already, with " + glfwManager);
        OpenClManager.glfwManager = glfwManager;

        if (instance == null) {
            instance = new OpenClManager();
        }
        return instance;
    }

    public long getClQueuePtr() {
        return clQueuePtr;
    }

    public long getClContextPtr() {
        return clContextPtr;
    }

    public long getClPlatform() {
        return clPlatform;
    }

    public long getClDevice() {
        return clDevice;
    }

    // Initialize Cl
    private void initCl() {

        // Initialize the OpenCL library
        // FIXME: seems like this has already been called somewhere
        // CL.create();

        // MemoryStack: https://github.com/LWJGL/lwjgl3-wiki/wiki/1.3.-Memory-FAQ,
        // https://blog.lwjgl.org/memory-management-in-lwjgl-3/
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer numPlatformsBuffer = stack.callocInt(1);
            int err = clGetPlatformIDs(null, numPlatformsBuffer);
            if (CL_SUCCESS != err) {
                throw new IllegalStateException("clGetPlatformIDs count failed with error code: " + err);
            }
            int numPlatforms = numPlatformsBuffer.get(0);

            // Allocate buffer for platform IDs
            PointerBuffer platforms = stack.mallocPointer(numPlatforms);
            err = clGetPlatformIDs(platforms, (IntBuffer) null);
            if (CL_SUCCESS != err) {
                throw new IllegalStateException("clGetPlatformIDs list failed with error code: " + err);
            }

            // Iterate over each platform ID
            for (int i = 0; i < numPlatforms; i++) {
                long platformID = platforms.get(i);
                log.fine(String.format("Platform %d ID: %d%n", i, platformID));

                // Example: fetch platform name length
                PointerBuffer sizeBuf = stack.mallocPointer(1);
                clGetPlatformInfo(platformID, CL_PLATFORM_NAME, (ByteBuffer) null, sizeBuf);

                // Fetch platform name
                ByteBuffer nameBuf = stack.malloc((int) sizeBuf.get(0));
                clGetPlatformInfo(platformID, CL_PLATFORM_NAME, nameBuf, null);
                byte[] nameBytes = new byte[nameBuf.remaining()];
                nameBuf.get(nameBytes);
                String platformName = new String(nameBytes).trim();

                log.info("Platform " + i + " Name: " + platformName);

                // --- Get Device (GPU) ---
                IntBuffer numDevices = stack.mallocInt(1);
                clGetDeviceIDs(platformID, CL_DEVICE_TYPE_GPU, null, numDevices);
                PointerBuffer devices = stack.mallocPointer(numDevices.get(0));
                clGetDeviceIDs(platformID, CL_DEVICE_TYPE_GPU, devices, (IntBuffer) null);

                clDevice = devices.get(0);
                clPlatform = platforms.get(0);
            }

            // --- Context Properties for GL Interop ---
            PointerBuffer ctxProps;

            PointerBuffer size = stack.mallocPointer(1);

            err = clGetPlatformInfo(
                    clPlatform,
                    CL_PLATFORM_EXTENSIONS,
                    (ByteBuffer) null,
                    size
            );

            if (err != CL_SUCCESS) {
                throw new IllegalStateException("Size query failed: " + err);
            }
            System.out.println("size: " + size.get(0));

            ByteBuffer buffer = stack.malloc((int) size.get(0));

            err = clGetPlatformInfo(
                    clPlatform,
                    CL_PLATFORM_EXTENSIONS,
                    buffer,
                    null
            );

            if (err != CL_SUCCESS) {
                throw new IllegalStateException("Info query failed: " + err);
            }

            String extensions = org.lwjgl.system.MemoryUtil.memUTF8(buffer);
            System.out.println("EXTS: " + extensions);


            PointerBuffer size1 = stack.mallocPointer(1);

            err = clGetPlatformInfo(
                    clPlatform,
                    CL_DEVICE_EXTENSIONS,
                    (ByteBuffer) null,
                    size1
            );

            if (err != CL_SUCCESS) {
                throw new IllegalStateException("Size query failed: " + err);
            }
            System.out.println("size: " + size1.get(0));

            ByteBuffer buffer1 = stack.malloc((int) size1.get(0));

            err = clGetPlatformInfo(
                    clPlatform,
                    CL_DEVICE_EXTENSIONS,
                    buffer1,
                    null
            );

            if (err != CL_SUCCESS) {
                throw new IllegalStateException("Info query failed: " + err);
            }

            extensions = org.lwjgl.system.MemoryUtil.memUTF8(buffer1);
            System.out.println("EXTS: " + extensions);
            if (!extensions.contains("cl_khr_gl_sharing")) {
                throw new IllegalStateException("Platform does not support cl_khr_gl_sharing");
            }


            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                long hwnd = GLFWNativeWin32.glfwGetWin32Window(glfwManager.getWindowPtr());
                long hdc = User32.GetDC(hwnd);
                ctxProps = stack.mallocPointer(7);
                ctxProps
                        .put(CL_CONTEXT_PLATFORM).put(clPlatform)
                        .put(CL_GL_CONTEXT_KHR).put(glfwGetWGLContext(glfwManager.getWindowPtr()))
                        .put(CL_WGL_HDC_KHR).put(hdc)
                        .put(0).flip();
                // TODO: now or later?
                User32.ReleaseDC(hwnd, hdc);
            } else if (os.contains("linux")) {
                ctxProps = stack.mallocPointer(7);
                ctxProps
                        .put(CL_CONTEXT_PLATFORM).put(clPlatform)
                        .put(CL_GL_CONTEXT_KHR).put(GLFWNativeGLX.glfwGetGLXContext(glfwManager.getWindowPtr()))
                        .put(CL_GLX_DISPLAY_KHR).put(GLFWNativeX11.glfwGetX11Display())
                        .put(0).flip();
            } else if (os.contains("mac")) {
                throw new UnsupportedOperationException("I admire your optimism!");
            } else {
                throw new UnsupportedOperationException("Unsupported OS for CL/GL interop");
            }

            // --- Create Context ---
            IntBuffer errcode = stack.callocInt(1);
            clContextPtr = clCreateContext(ctxProps, clDevice, null, 0, errcode);
            if (clContextPtr == 0L || errcode.get(0) != CL_SUCCESS) {
                throw new IllegalStateException("Failed to create CL context: " + errcode.get(0));
            }

            // --- Command Queue ---
            clQueuePtr = clCreateCommandQueue(clContextPtr, clDevice, 0, errcode);
            if (clQueuePtr == 0L) throw new IllegalStateException("Failed to create CL queue");

            isInitialized = true;
            log.info("OpenCL + OpenGL sharing initialized!");
        }
    }


    public boolean isInitialized() {
        return isInitialized;
    }

    public void dispose() {
        clReleaseCommandQueue(clQueuePtr);
        clReleaseContext(clContextPtr);

        CL.destroy();
    }
}
