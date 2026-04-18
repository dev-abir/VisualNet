package org.devabir.common;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Entity {

    private final String name;

    private final Vector3f position;
    private final Vector3f rotation;
    private final Vector3f scale;

    private final Matrix4f modelMatrix;

    public Entity(String name) {
        this.name = name;

        this.position = new Vector3f(0.0f, 0.0f, 0.0f);
        this.rotation = new Vector3f(0.0f, 0.0f, 0.0f);
        this.scale = new Vector3f(1.0f, 1.0f, 1.0f);

        this.modelMatrix = new Matrix4f();
        updateModelMatrix();
    }

    // Update the model matrix based on position, rotation, and scale
    public void updateModelMatrix() {
        modelMatrix.identity();

        modelMatrix.translate(position);
        modelMatrix.rotateXYZ(rotation.x, rotation.y, rotation.z);
        modelMatrix.scale(scale);
    }

    public String getName() {
        return this.name;
    }

    public Matrix4f getModelMatrix() {
        return new Matrix4f(modelMatrix);
    }

    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
        updateModelMatrix();
    }

    public void setRotation(float pitch, float yaw, float roll) {
        this.rotation.set(pitch, yaw, roll);
        updateModelMatrix();
    }

    public void setScale(float x, float y, float z) {
        this.scale.set(x, y, z);
        updateModelMatrix();
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getRotation() {
        return rotation;
    }

    public Vector3f getScale() {
        return scale;
    }
}
