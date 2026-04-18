#version 460 core

// small quad vertex positions
layout(location = 0) in vec3 aPos;

// pass pixel index to fragment shader
// (this is coming from the instanced rendering thing)
flat out int pixelIndex;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
uniform float scale;

// which MNIST image to render (0-based)
uniform int imageIndex;

void main() {
    // gl_InstanceID represents the pixel index (0..783) of the OG image.
    // It will stay at 0 for first 6 invocations (6 verts)
    // then progresses to 1, then stay for 6 invoc., then 2 and so on..
    pixelIndex = gl_InstanceID;

    int px = gl_InstanceID % 28;
    int py = gl_InstanceID / 28;

    // scale and offset each pixel to form the full 28x28 squares
    vec3 pos = aPos * scale;
    pos.x += px * scale;
    pos.y -= py * scale;

    gl_Position = projection * view * model * vec4(pos, 1.0);
}