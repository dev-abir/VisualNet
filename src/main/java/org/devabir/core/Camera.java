package org.devabir.core;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {
    // Position & orientation
    private final Vector3f position = new Vector3f(0, 10, 20);
    private final Vector3f front = new Vector3f(0, 0, -1);
    private final Vector3f up = new Vector3f(0, 1, 0);
    private final Vector3f right = new Vector3f(1, 0, 0);

    // Euler angles
    private float yaw = -90.0f;
    private float pitch = -30.0f;

    // Movement
    private final Vector3f velocity = new Vector3f();
    private final float moveSpeed = 100.0f;
    private final float damping = 10.0f; // higher = tighter, lower = floaty

    // ====== ROTATION ======
    private float keyboardLookSpeed = 10.0f; // degrees/sec
    private float yawVel = 0.0f;
    private float pitchVel = 0.0f;
    private float lookDamping = 10.0f;

//    // Mouse
//    private final float mouseSensitivity = 0.12f;

    private final Matrix4f viewMatrix = new Matrix4f();

    public Camera() {
        updateVectors();
    }

    // Call every frame
    public void update(float deltaTime) {
        // Smooth movement (non-robotic)
        position.fma(deltaTime, velocity);
        velocity.lerp(new Vector3f(0, 0, 0), damping * deltaTime);

//        float sway = (float)Math.sin(time * 0.8f) * 0.02f;
//        position.y += sway;

        // Smooth rotation
        yaw += yawVel * deltaTime;
        pitch += pitchVel * deltaTime;

        // Clamp pitch
        pitch = Math.max(-89.0f, Math.min(89.0f, pitch));

        // Apply damping to rotation
        yawVel *= Math.exp(-lookDamping * deltaTime);
        pitchVel *= Math.exp(-lookDamping * deltaTime);

        updateVectors();
    }

    public Matrix4f getViewMatrix() {
        return viewMatrix.setLookAt(
                position,
                new Vector3f(position).add(front),
                up
        );
    }

    // ================= MOVEMENT =================

    public void moveForward(float deltaTime) {
        velocity.fma(moveSpeed * deltaTime, front);
    }

    public void moveBackward(float deltaTime) {
        velocity.fma(-moveSpeed * deltaTime, front);
    }

    public void moveRight(float deltaTime) {
        velocity.fma(moveSpeed * deltaTime, right);
    }

    public void moveLeft(float deltaTime) {
        velocity.fma(-moveSpeed * deltaTime, right);
    }

    public void moveUp(float deltaTime) {
        velocity.y += moveSpeed * deltaTime;
    }

    public void moveDown(float deltaTime) {
        velocity.y -= moveSpeed * deltaTime;
    }

    // ====== ROTATION CONTROLS ======
    public void yawLeft(float deltaTime) {
        yawVel -= keyboardLookSpeed;
    }

    public void yawRight(float deltaTime) {
        yawVel += keyboardLookSpeed;
    }

    public void pitchUp(float deltaTime) {
        pitchVel += keyboardLookSpeed;
    }

    public void pitchDown(float deltaTime) {
        pitchVel -= keyboardLookSpeed;
    }

//    // ================= MOUSE =================
//
//    public void mouseMove(float dx, float dy) {
//        dx *= mouseSensitivity;
//        dy *= mouseSensitivity;
//
//        yaw   += dx;
//        pitch += dy;
//
//        pitch = Math.max(-89.0f, Math.min(89.0f, pitch));
//    }

    // ================= INTERNAL =================

    private void updateVectors() {
        front.set(
                (float) Math.cos(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch)),
                (float) Math.sin(Math.toRadians(pitch)),
                (float) Math.sin(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch))
        ).normalize();

        right.set(front).cross(0, 1, 0).normalize();
        up.set(right).cross(front).normalize();
    }

    // Optional accessors
    public Vector3f getPosition() {
        return position;
    }
}
