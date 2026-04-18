package org.devabir;

import org.devabir.common.*;
import org.devabir.core.Camera;
import org.devabir.core.GlfwManager;
import org.devabir.core.OpenClManager;
import org.devabir.core.OpenGlManager;
import org.devabir.dataset.MnistDataset;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CL10GL;
import org.lwjgl.opencl.CL20;
import org.lwjgl.system.MemoryStack;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Random;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.opencl.CL10GL.clEnqueueAcquireGLObjects;
import static org.lwjgl.opencl.CL10GL.clEnqueueReleaseGLObjects;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL43.*;

public class Main {

    private static final int WINDOW_WIDTH = 500;
    private static final int WINDOW_HEIGHT = WINDOW_WIDTH;
    private static final float FOV = 60.0f;
    private static final float Z_NEAR = 0.01f;
    private static final float Z_FAR = 100.0f;

    public static void main(String[] args) throws IOException {
        System.out.println("Hello");
        GlfwManager glfwManager = GlfwManager.getInstance(WINDOW_WIDTH, WINDOW_HEIGHT);
        OpenGlManager openGlManager = OpenGlManager.getInstance();
        // OpenClManager clManager = OpenClManager.getInstance(glfwManager);
        // clManager.dispose();
        Properties props = new Properties();
        // Files.newInputStream(Path.of("application.properties"))
        try (var fis = new FileInputStream("application.properties")) {
            props.load(fis);
        }
        MnistDataset mnistDataset = new MnistDataset(
                props.getProperty("KAGGLE_USERNAME"),
                props.getProperty("KAGGLE_KEY"),
                props.getProperty("DATASET_DOWNLOAD_DIR")
        );
        try {
            IdxFile mnistData = mnistDataset.hack();
//            vpLoc = glGetUniformLocation(program, "uVP");
//            float[] squareVertices = {
//                    -0.5f, -0.5f, 0f,
//                    0.5f, -0.5f, 0f,
//                    0.5f,  0.5f, 0f,
//                    -0.5f,  0.5f, 0f
//            };
            float[] squareVertices = {
                    // first triangle
                    -0.5f, -0.5f, 0f,
                    0.5f, -0.5f, 0f,
                    0.5f, 0.5f, 0f,
                    // second triangle
                    0.5f, 0.5f, 0f,
                    -0.5f, 0.5f, 0f,
                    -0.5f, -0.5f, 0f,
            };
            int[] squareIndices = {
                    0,1,2,
                    2,3,0
            };
            var squareVao = new GLVao("square");
            squareVao.bind();

            var squareVertsBuffer = new GLBuffer("square_verts", GL_ARRAY_BUFFER, GL_STATIC_DRAW);
                squareVertsBuffer.upload(squareVertices);

            squareVao.attachAttribute(
                    squareVertsBuffer,
                    0,
                    3,
                    GL_FLOAT,
                    false,
                    3*Float.BYTES,
                    0
            );

            var squareIndicesBuffer = new GLBuffer("square_indices", GL_ELEMENT_ARRAY_BUFFER, GL_STATIC_DRAW);
            squareIndicesBuffer.upload(squareIndices);

            var mnistDataSSBO = new GLBuffer("mnist_data", GL_SHADER_STORAGE_BUFFER, GL_STATIC_DRAW);
            mnistDataSSBO.upload(mnistData.getData());
            mnistDataSSBO.bindBase(0);

            GLBuffer[] weightSSBOs = new GLBuffer[] {
                    new GLBuffer("weights0", GL_SHADER_STORAGE_BUFFER, GL_DYNAMIC_DRAW),
                    new GLBuffer("weights1", GL_SHADER_STORAGE_BUFFER, GL_DYNAMIC_DRAW),
                    new GLBuffer("weights2", GL_SHADER_STORAGE_BUFFER, GL_DYNAMIC_DRAW)
            };
            float[] randomFloats = new float[28*28*64*64];
            Random rnd = new Random();
            for (int i = 0; i < randomFloats.length; i++) {
                randomFloats[i] = rnd.nextFloat();
            }
            weightSSBOs[0].upload(randomFloats);
            weightSSBOs[0].bindBase(1);

            GLBuffer[] activationSSBOs = new GLBuffer[] {
                    new GLBuffer("activations0", GL_SHADER_STORAGE_BUFFER, GL_DYNAMIC_DRAW),
                    new GLBuffer("activations1", GL_SHADER_STORAGE_BUFFER, GL_DYNAMIC_DRAW)
            };
            activationSSBOs[0].allocate(64*64*Float.BYTES);
            activationSSBOs[0].bindBase(2);

            GLBuffer[] edgeSSBOs = new GLBuffer[] {
                    new GLBuffer("edges0", GL_SHADER_STORAGE_BUFFER, GL_DYNAMIC_DRAW),
                    new GLBuffer("edges1", GL_SHADER_STORAGE_BUFFER, GL_DYNAMIC_DRAW),
                    new GLBuffer("edges2", GL_SHADER_STORAGE_BUFFER, GL_DYNAMIC_DRAW)
            };
            edgeSSBOs[0].allocate(7*7*16*16*Float.BYTES);
            edgeSSBOs[0].bindBase(3);

            String inputLayerVertexSrc, inputLayerFragmentSrc;
            try (var in = Main.class.getResourceAsStream("/input_layer.vsh")) {
                assert in != null;
                inputLayerVertexSrc = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            try (var in = Main.class.getResourceAsStream("/input_layer.fsh")) {
                assert in != null;
                inputLayerFragmentSrc = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }

            GlShader inputLayerShader = new GlShader.Builder()
                    .attachVertex(inputLayerVertexSrc)
                    .attachFragment(inputLayerFragmentSrc)
                    .link();
            inputLayerShader.use();

                var projection = new Matrix4f()
                        .setPerspective(
                                (float) Math.toRadians(FOV),
                                (float) glfwManager.getWindowWidth() / glfwManager.getWindowHeight(),
                                Z_NEAR,
                                Z_FAR);

                float scale = 0.5f;
                var model = new Matrix4f()
                        .translate(-0.5f*14,0.5f*14,0)
//                        .rotateY((float) Math.toRadians(30))
                        .scale(1);

            int projLoc = glGetUniformLocation(inputLayerShader.getId(), "projection");
            int viewLoc = glGetUniformLocation(inputLayerShader.getId(), "view");
            int modelLoc = glGetUniformLocation(inputLayerShader.getId(), "model");
            int imageIndexLoc = glGetUniformLocation(inputLayerShader.getId(), "imageIndex");
            int scaleLoc = glGetUniformLocation(inputLayerShader.getId(), "scale");

            squareVao.unbind();

            String feedForwardCompSrc;
            try (var in = Main.class.getResourceAsStream("/feed_forward.comp")) {
                assert in != null;
                feedForwardCompSrc = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            GlShader feedForwardShader = new GlShader.Builder()
                    .attachCompute(feedForwardCompSrc)
                    .link();

            String maxPoolingCompSrc;
            try (var in = Main.class.getResourceAsStream("/max_pooling.comp")) {
                assert in != null;
                maxPoolingCompSrc = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            GlShader maxPoolingShader = new GlShader.Builder()
                    .attachCompute(maxPoolingCompSrc)
                    .link();

            String weightEdgeRenderVertexSrc, weightEdgeRenderFragmentSrc;
            try (var in = Main.class.getResourceAsStream("/weight_edges.vsh")) {
                assert in != null;
                weightEdgeRenderVertexSrc = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            try (var in = Main.class.getResourceAsStream("/weight_edges.fsh")) {
                assert in != null;
                weightEdgeRenderFragmentSrc = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            GlShader weightEdgeRenderShader = new GlShader.Builder()
                    .attachVertex(weightEdgeRenderVertexSrc)
                    .attachFragment(weightEdgeRenderFragmentSrc)
                    .link();

            var modelEdges = new Matrix4f()
                    .translate(0,0,7)
//                    .rotateY((float) Math.toRadians(30))
                    .scale(1);

            int weightEdgesModelLoc = glGetUniformLocation(weightEdgeRenderShader.getId(), "model");
            int weightEdgesProjLoc = glGetUniformLocation(weightEdgeRenderShader.getId(), "projection");
            int weightEdgesViewLoc = glGetUniformLocation(weightEdgeRenderShader.getId(), "view");

//            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glEnable(GL_LINE_SMOOTH);

            int imageIdx = 2; // use the 3rd image

            glfwSetTime(0);
            float lastTime = 0.0f;

            Camera camera = new Camera();

            float[] lol = new float[7*7*16*16];

            while (!GLFW.glfwWindowShouldClose(glfwManager.getWindowPtr())) {
                System.out.print("\rTime: " + glfwGetTime());
                imageIdx = (int) glfwGetTime() % mnistData.getDimensions()[0];
                glClearColor(0.1f,0.1f,0.1f, 1.0f);
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                inputLayerShader.use();
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    var projectionBuffer = projection.get(stack.mallocFloat(16));
                    var viewBuffer = camera.getViewMatrix().get(stack.mallocFloat(16));
                    var modelBuffer = model.get(stack.mallocFloat(16));
                    glUniformMatrix4fv(projLoc, false, projectionBuffer);
                    glUniformMatrix4fv(viewLoc, false, viewBuffer);
                    glUniformMatrix4fv(modelLoc, false, modelBuffer);
                }
                glUniform1i(imageIndexLoc, imageIdx);
                glUniform1f(scaleLoc, scale);
                squareVao.bind();
//                glDrawElementsInstanced(
//                    GL_TRIANGLES,   // draw triangles
//                    6,              // number of indices per instance (2 triangles)
//                    GL_UNSIGNED_INT,// type of the indices
//                    0,              // offset in the index buffer
//                    28*28           // number of instances (pixels)
//                );
                squareVao.renderInstanced(GL_TRIANGLES, squareVertices.length, 28 * 28);

                feedForwardShader.use();
                glUniform1i(glGetUniformLocation(feedForwardShader.getId(), "inputSize"), 28*28);
                glUniform1i(glGetUniformLocation(feedForwardShader.getId(), "hiddenCount"), 64*64);
                glUniform1i(glGetUniformLocation(feedForwardShader.getId(), "imageIndex"), imageIdx);
                int threadsPerGroup = 256;
                int groups = (int) Math.ceil((64.0 * 64.0) / threadsPerGroup);
                glDispatchCompute(groups, 1, 1);
                glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);

                // The weight array[4096][784] will be like:
                // [weights for hidden 0 (28 * 28 els.),
                // weights for hidden 1 (784 els.), ...
                // weights for hidden 254 (784 els.),
                // weights for hidden 255 (784 els.)]

                maxPoolingShader.use();
                glUniform1i(glGetUniformLocation(maxPoolingShader.getId(), "imageIndex"), imageIdx);
                glDispatchCompute(7 * 7, 16 * 16, 1);
                glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT | GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT);

//                glGetBufferSubData(GL_SHADER_STORAGE_BUFFER,0,lol);

                // square vao (dummy in this case) is already active
                weightEdgeRenderShader.use();
                glUniform1f(glGetUniformLocation(weightEdgeRenderShader.getId(), "time"), (float) glfwGetTime());
//                modelEdges.translate(0,0,(float)(glfwGetTime() / 10.0f));
                modelEdges.setRotationXYZ(0,(float) 0,0);
                modelEdges.scale(5,-5,5);
                glUniformMatrix4fv(weightEdgesModelLoc, false, modelEdges.get(new float[16]));
                glUniformMatrix4fv(weightEdgesViewLoc, false, camera.getViewMatrix().get(new float[16]));
                glUniformMatrix4fv(weightEdgesProjLoc, false, projection.get(new float[16]));
                squareVao.renderInstanced(GL_LINES, 2, 7 * 7 * 16 * 16);
//                squareVao.renderInstanced(GL_TRIANGLE_STRIP, 4, 7 * 7 * 16 * 16);
//                squareVao.renderInstanced(GL_TRIANGLE_STRIP, 4, 7 * 7 * 16 * 16);
//            glDrawArraysInstanced(
//                    GL_TRIANGLE_STRIP,
//                    0,
//                    4,
//                    7*7*16*16
//            );

                float currentTime = (float) glfwGetTime();
                float deltaTime = currentTime - lastTime;
                lastTime = currentTime;

                if (glfwManager.isKeyPressed(GLFW_KEY_W)) camera.moveForward(deltaTime);
                if (glfwManager.isKeyPressed(GLFW_KEY_S)) camera.moveBackward(deltaTime);
                if (glfwManager.isKeyPressed(GLFW_KEY_A)) camera.moveLeft(deltaTime);
                if (glfwManager.isKeyPressed(GLFW_KEY_D)) camera.moveRight(deltaTime);
                if (glfwManager.isKeyPressed(GLFW_KEY_SPACE)) camera.moveUp(deltaTime);
                if (glfwManager.isKeyPressed(GLFW_KEY_LEFT_SHIFT)) camera.moveDown(deltaTime);

                if (glfwManager.isKeyPressed(GLFW_KEY_LEFT)) camera.yawLeft(deltaTime);
                if (glfwManager.isKeyPressed(GLFW_KEY_RIGHT)) camera.yawRight(deltaTime);
                if (glfwManager.isKeyPressed(GLFW_KEY_UP)) camera.pitchUp(deltaTime);
                if (glfwManager.isKeyPressed(GLFW_KEY_DOWN)) camera.pitchDown(deltaTime);

                if (glfwManager.isKeyPressed(GLFW_KEY_ESCAPE)) {
                    glfwSetWindowShouldClose(glfwManager.getWindowPtr(), true);
                }

                camera.update(deltaTime);

                GLFW.glfwSwapBuffers(glfwManager.getWindowPtr());
                GLFW.glfwPollEvents();
            }

            feedForwardShader.dispose();
            maxPoolingShader.dispose();
            inputLayerShader.dispose();
            squareVao.dispose();
            glfwManager.dispose();

//            for (int i = 0; i < 16 * 16; i++) {
//                for (int j = 0; j < 7 * 7; j++) {
//                    System.out.print(lol[i * 7 * 7 + j] + " ");
//                }
//                System.out.println();
//            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void gain(String[] args) throws Exception {
        // GLFW.glfwInit();
        // long win = GLFW.glfwCreateWindow(800, 600, "GL+CL", NULL, NULL);
        // GLFW.glfwMakeContextCurrent(win);


        System.out.println("Hello");
        GlfwManager glfwManager = GlfwManager.getInstance(WINDOW_WIDTH, WINDOW_HEIGHT);
        OpenGlManager openGlManager = OpenGlManager.getInstance();

        // GL.createCapabilities();

        float[] verts = {0, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f};
        int componentsPerVertex = 2;
        // int vao = glGenVertexArrays(); glBindVertexArray(vao);
        // int vbo = glGenBuffers(); glBindBuffer(GL_ARRAY_BUFFER, vbo);
        GlArray glArray = new GlArray("triangle_verts", verts, componentsPerVertex, GL_STATIC_DRAW, 0);
        // glBufferData(GL_ARRAY_BUFFER, verts, GL_DYNAMIC_DRAW);
        // glVertexAttribPointer(0, componentsPerVertex, GL_FLOAT, false, 0, 0);
        // glEnableVertexAttribArray(0);

        // --- Create shaders ---
        String vertexSrc = "#version 330 core\n" +
                           "layout(location = 0) in vec2 position;\n" +
                           "void main() {\n" +
                           "    gl_Position = vec4(position, 0.0, 1.0);\n" +
                           "}\n";

        String fragmentSrc = "#version 330 core\n" +
                             "out vec4 FragColor;\n" +
                             "void main() {\n" +
                             "    FragColor = vec4(1.0, 0.5, 0.0, 1.0); // orange\n" +
                             "}\n";

        GlShader shader = new GlShader.Builder()
                .attachVertex(vertexSrc)
                .attachFragment(fragmentSrc)
                .link();

        IntBuffer err = BufferUtils.createIntBuffer(1);
        // CL.create();
        // CLPlatform platform = CLPlatform.getPlatforms().get(0);
        // CLDevice device = platform.getDevices(CL_DEVICE_TYPE_GPU).get(0);
        //
        // PointerBuffer props = BufferUtils.createPointerBuffer(7);
        // props.put(CL_CONTEXT_PLATFORM).put(platform.getPointer());
        // props.put(CL_GL_CONTEXT_KHR).put(GL.getCapabilities().glGetString(GL_VERSION));
        // props.put(CL_WGL_HDC_KHR).put(GLFWNativeWGL.glfwGetWGLContext(win));
        // props.put(0).flip();

        OpenClManager clManager = OpenClManager.getInstance(glfwManager);
        // clManager.dispose();

        // long ctx =  CLContext.create(platform, device, props, null, err);
        long mem = CL10GL.clCreateFromGLBuffer(clManager.getClContextPtr(), CL_MEM_READ_WRITE, glArray.getVboId(), err);

        String src = "__kernel void _vnet_rotate(__global float2* v, float a) {\n" +
                     "    float c=cos(a), s=sin(a);\n" +
                     "    int i=get_global_id(0);\n" +
                     "    float2 p=v[i];\n" +
                     "    v[i]=(float2)(p.x*c - p.y*s, p.x*s + p.y*c);\n" +
                     "}";
        long program = clCreateProgramWithSource(clManager.getClContextPtr(), src, err);
        int result = clBuildProgram(program, clManager.getClDevice(), "", (prog, user_data) -> {
// First, query log size
            PointerBuffer logSize = BufferUtils.createPointerBuffer(1);
            CL20.clGetProgramBuildInfo(prog, clManager.getClDevice(), CL20.CL_PROGRAM_BUILD_LOG, (ByteBuffer) null, logSize);
            long size = logSize.get(0);

            // Allocate a *direct* ByteBuffer with the right capacity
            ByteBuffer logBuffer = BufferUtils.createByteBuffer((int) size);
            CL20.clGetProgramBuildInfo(prog, clManager.getClDevice(), CL20.CL_PROGRAM_BUILD_LOG, logBuffer, null);

            // Read the string safely
            byte[] bytes = new byte[(int) size - 1]; // minus null terminator
            logBuffer.get(bytes);
            System.out.println("CL Build Log:\n" + new String(bytes));
        }, 0);
        System.out.println("hehe "+ result+" "+err.get(0));
        long kernel = clCreateKernel(program, "_vnet_rotate", err);

        FloatBuffer ang = BufferUtils.createFloatBuffer(1);
        PointerBuffer globalWorkSize = BufferUtils.createPointerBuffer(1).put(0, 3);
        while (!GLFW.glfwWindowShouldClose(glfwManager.getWindowPtr())) {
            clEnqueueAcquireGLObjects(clManager.getClQueuePtr(), mem, null, null);
            // ang.put(0, (float)Math.toRadians(15));
            clSetKernelArg1p(kernel, 0, mem);
            float angle = (float) Math.toRadians(1);
            clSetKernelArg1f(kernel, 1, angle);
            // clSetKernelArg(kernel, 1, ang);
            int errCl = clEnqueueNDRangeKernel(clManager.getClQueuePtr(), kernel, 1, null, globalWorkSize, null, null, null);
            if (errCl != CL10.CL_SUCCESS) {
                System.err.println("Kernel enqueue failed: " + errCl);
            }
            clEnqueueReleaseGLObjects(clManager.getClQueuePtr(), mem, null, null);
            clFinish(clManager.getClQueuePtr());

            glClear(GL_COLOR_BUFFER_BIT);
            // glColor3f(1.0f, 0.5f, 0.0f); // Orange
            shader.use();
            glArray.render(GL_TRIANGLES);
            // glDrawArrays(GL_TRIANGLES, 0, 3);
            GLFW.glfwSwapBuffers(glfwManager.getWindowPtr());
            GLFW.glfwPollEvents();
        }

        clManager.dispose();
        shader.dispose();
        glArray.dispose();
        glfwManager.dispose();
    }
}