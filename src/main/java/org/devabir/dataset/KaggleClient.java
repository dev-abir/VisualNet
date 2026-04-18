package org.devabir.dataset;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

/**
 * Utility class to interact with the Kaggle API, download and decompress datasets
 */
public class KaggleClient {

    // private static final Logger log = LoggerFactory.getLogger(KaggleClient.class.getName());

    public static final int UNZIP_BUFFER_SIZE = 2048;

    private final String username;
    private final String apiKey;

    /**
     * Check Kaggle auth docs: <a href="https://www.kaggle.com/docs/api">Kaggle API</a>
     */
    public KaggleClient(String username, String apiKey) {
        this.username = username;
        this.apiKey = apiKey;
    }

    /**
     * Downloads the dataset from Kaggle.
     *
     * @param downloadUrl the dataset download URL
     * @param outputZip   desired output zip location
     * @throws IOException if non-success response is sent from Kaggle or there's an issue while creating the zip file
     */
    public void downloadDataset(String downloadUrl, String outputZip) throws IOException {
        System.out.println("Downloading dataset from " + downloadUrl + "\nWriting to: " + outputZip);

        URL url = new URL(downloadUrl);
        String credentials = username + ":" + apiKey;
        String authHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", authHeader);
        connection.connect();

        try (
                InputStream inputStream = connection.getInputStream();
                ReadableByteChannel rbc = Channels.newChannel(inputStream);
                FileOutputStream fos = new FileOutputStream(outputZip)
        ) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }

        connection.disconnect();
        System.out.println("Download done");
    }

    /**
     * Unzips the zip file at zipFilePath to destinationDirName
     *
     * @param zipFilePath        the file path to the zip
     * @param destinationDirName the desired extraction location
     * @throws FileNotFoundException if the zip file isn't present
     * @throws ZipException          while extracting the zip file
     * @throws IOException           while writing the extracted contents to the zip file
     */
    public void unzip(String zipFilePath, String destinationDirName) throws FileNotFoundException, ZipException, IOException {
        System.out.println("Unzip " + zipFilePath);

        try (var zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry zipEntry;
            byte[] buffer = new byte[UNZIP_BUFFER_SIZE];
            while ((zipEntry = zis.getNextEntry()) != null) {
                File newFile = newFile(destinationDirName, zipEntry);
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    // Fix for Windows-created archives
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }

                    long entrySize = zipEntry.getSize(); // uncompressed size
                    long written = 0;

                    // Write file content
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                            // Progress tracking
                            written += len;
                            if (entrySize <= 0) {
                                continue;
                            }
                            int percent = (int) ((written * 100) / entrySize);
                            // log.info("\rExtracting {} : {}%", zipEntry.getName(), percent);
                            System.out.print("\rExtracting " + zipEntry.getName() + " : " + percent + "%");
                        }
                    }
                }
            }
        }
    }

    /**
     * Prevents a Zip Slip vulnerability.
     * If the ZIP entry tries to escape that directory
     * (using ../ tricks), it throws an exception.
     */
    private File newFile(String destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir + File.separator + zipEntry.getName());

        String destinationFilePath = destFile.getCanonicalPath();
        // System.out.println(destinationFilePath + " " + destinationDir);

        if (!destinationFilePath.startsWith(new File(destinationDir).getCanonicalPath() + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
}
//
//
//import org.lwjgl .*;
//        import org.lwjgl.glfw .*;
//        import org.lwjgl.opengl .*;
//        import org.lwjgl.opencl .*;
//        import org.lwjgl.system .*;
//
//        import java.nio.FloatBuffer;
//
//import static org.lwjgl.glfw.Callbacks .*;
//        import static org.lwjgl.glfw.GLFW .*;
//        import static org.lwjgl.opengl.GL33C .*;
//        import static org.lwjgl.opencl.CL10 .*;
//        import static org.lwjgl.opencl.CL10GL .*;
//        import static org.lwjgl.system.MemoryUtil .*;
//
//public class OpenCLGLInterop {
//    private static final String KERNEL_SRC =
//            "__kernel void rotate(__global float2* vertices, float angle) {\n" +
//                    "   int i = get_global_id(0);\n" +
//                    "   float2 v = vertices[i];\n" +
//                    "   float c = cos(angle);\n" +
//                    "   float s = sin(angle);\n" +
//                    "   float x = v.x * c - v.y * s;\n" +
//                    "   float y = v.x * s + v.y * c;\n" +
//                    "   vertices[i] = (float2)(x, y);\n" +
//                    "}";
//    private long window;
//    private int vbo;
//    private long clContext;
//    private long clQueue;
//    private long clProgram;
//    private long clKernel;
//    private long clMem;
//
//    public static void main(String[] args) {
//        new OpenCLGLInterop().run();
//    }
//
//    public void run() {
//        initWindow();
//        initOpenGL();
//        initOpenCL();
//
//        loop();
//
//        cleanup();
//    }
//
//    private void initWindow() {
//        if (!glfwInit()) throw new IllegalStateException("Unable to init GLFW");
//
//        glfwDefaultWindowHints();
//        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
//        window = glfwCreateWindow(800, 600, "OpenCL-OpenGL Interop", NULL, NULL);
//        if (window == NULL) throw new RuntimeException("Failed to create window");
//
//        glfwMakeContextCurrent(window);
//        glfwSwapInterval(1);
//        glfwShowWindow(window);
//    }
//
//    private void initOpenGL() {
//        GL.createCapabilities();
//
//        // Quad vertices (4 corners)
//        float[] quad = {
//                -0.5f, -0.5f,
//                0.5f, -0.5f,
//                0.5f, 0.5f,
//                -0.5f, 0.5f
//        };
//
//        vbo = glGenBuffers();
//        glBindBuffer(GL_ARRAY_BUFFER, vbo);
//        glBufferData(GL_ARRAY_BUFFER, quad, GL_DYNAMIC_DRAW);
//        glBindBuffer(GL_ARRAY_BUFFER, 0);
//    }
//
//    private void initOpenCL() {
//        // Get platform & device
//        CL.create();
//        long platform = CLPlatform.getPlatforms().get(0);
//        long device = CLPlatform.getPlatforms().get(0).getDevices(CL_DEVICE_TYPE_GPU).get(0);
//
//        // Create OpenCL context sharing with OpenGL
//        clContext = CLContext.create(platform, device, new CLContextCallback() {
//            @Override
//            public void invoke(long errinfo, long private_info, long cb, long user_data) {
//                System.err.println("CL Error: " + memUTF8(errinfo));
//            }
//        }, window);
//
//        clQueue = clCreateCommandQueue(clContext, device, 0, null);
//
//        // Create OpenCL buffer from OpenGL VBO
//        clMem = clCreateFromGLBuffer(clContext, CL_MEM_READ_WRITE, vbo, null);
//
//        // Build program
//        clProgram = CLUtils.createProgram(clContext, device, KERNEL_SRC);
//        clKernel = clCreateKernel(clProgram, "rotate", null);
//    }
//
//    private void loop() {
//        while (!glfwWindowShouldClose(window)) {
//            glfwPollEvents();
//
//            float angle = (float) (System.currentTimeMillis() % 5000L / 5000.0 * 2 * Math.PI);
//
//            // Acquire GL buffer in CL
//            clEnqueueAcquireGLObjects(clQueue, clMem, null, null);
//
//            // Set args and run kernel
//            clSetKernelArg(clKernel, 0, clMem);
//            clSetKernelArg(clKernel, 1, angle);
//            clEnqueueNDRangeKernel(clQueue, clKernel, 1, null, new long[]{4}, null, null, null);
//            clFinish(clQueue);
//
//            // Release GL buffer in CL
//            clEnqueueReleaseGLObjects(clQueue, clMem, null, null);
//            clFinish(clQueue);
//
//            // Render with OpenGL
//            glClear(GL_COLOR_BUFFER_BIT);
//            glBindBuffer(GL_ARRAY_BUFFER, vbo);
//            glEnableVertexAttribArray(0);
//            glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
//            glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
//            glDisableVertexAttribArray(0);
//            glBindBuffer(GL_ARRAY_BUFFER, 0);
//
//            glfwSwapBuffers(window);
//        }
//    }
//
//    private void cleanup() {
//        clReleaseKernel(clKernel);
//        clReleaseProgram(clProgram);
//        clReleaseMemObject(clMem);
//        clReleaseCommandQueue(clQueue);
//        clReleaseContext(clContext);
//
//        glDeleteBuffers(vbo);
//        glfwFreeCallbacks(window);
//        glfwDestroyWindow(window);
//        glfwTerminate();
//    }
//}
//
/// / Small helper for CL program compilation
//class CLUtils {
//    public static long createProgram(long context, long device, String src) {
//        long program = CL10.clCreateProgramWithSource(context, src, null);
//        int err = CL10.clBuildProgram(program, device, "", null, NULL);
//        if (err != CL10.CL_SUCCESS) {
//            ByteBuffer log = CL10.clGetProgramBuildInfo(program, device, CL10.CL_PROGRAM_BUILD_LOG);
//            throw new RuntimeException("CL Build failed:\n" + MemoryUtil.memUTF8(log));
//        }
//        return program;
//    }
//}
//
