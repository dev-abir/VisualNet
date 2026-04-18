#version 460 core

out vec4 FragColor;

flat in int pixelIndex;

layout(std430, binding = 0) buffer MNISTData {
    float mnist[]; // nImages * 28 * 28 floats
};

// which MNIST image to render
uniform int imageIndex;

float brightnessFactor = 0.1;

void main() {
    int mnistIdx = imageIndex * 28 * 28 + pixelIndex;
    float value = clamp(mnist[mnistIdx] + brightnessFactor, 0.0, 1.0);
    FragColor = vec4(value, value, value, 1.0);
}