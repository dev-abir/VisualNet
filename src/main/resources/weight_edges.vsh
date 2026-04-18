//#version 430
//
//layout(std430, binding = 3) buffer RenderSSBO {
//    float edgeStrength[]; // [256 * 49]
//};
//
//uniform mat4 model;
//uniform mat4 view;
//uniform mat4 projection;
//
//out float vStrength;
//out float vT;
//out vec4 worldPos;
//
//void main() {
//    uint id = gl_InstanceID;   // 0 .. (256*49 - 1)
//    uint v  = gl_VertexID;     // 0..3
//
//    uint pooledInputIdx  = id % 49;
//    uint pooledHiddenIdx = id / 49;
//
//    vec2 srcGrid = vec2(pooledInputIdx % 7,  pooledInputIdx / 7);
//    vec2 dstGrid = vec2(pooledHiddenIdx % 16, pooledHiddenIdx / 16);
//
//    // srcGrid : x,y ∈ [0 .. 6]
//    // Divide by (N - 1) to map grid indices into [0, 1]
//    // Subtract 0.5 to center the grid around the origin
//    //    [0, 1] → [-0.5, +0.5]
//    vec3 srcPos = vec3(
//        srcGrid / 6.0 - 0.5,    // normalized, centered X/Y position
//        -0.5                    // input layer depth
//    );
//    vec3 dstPos = vec3(dstGrid / 15.0 - 0.5,  0.5);
//
//    vec3 dir = normalize(dstPos - srcPos);
//
//    // Camera-facing perpendicular
//    vec3 viewDir = normalize((inverse(view) * vec4(0, 0, -1, 0)).xyz);
//    vec3 right   = normalize(cross(dir, viewDir));
//
//    float strength = edgeStrength[id];
//    float thickness = mix(0.002, 0.02, clamp(abs(strength), 0.0, 1.0));
//
//    // Vertex layout
//    // Triangle strip:
//    // 0 (0,-1)
//    // 1 (0,+1)
//    // 2 (1,-1)
//    // 3 (1,+1)
//    bool isEnd   = (v >= 2);
//    bool isRight = (v % 2 != 0);
//
//    vec3 basePos = isEnd ? dstPos : srcPos;
//    float side   = isRight ? 1.0 : -1.0;
//
//    float taper = isEnd ? 0.7 : 1.0;
//
//    vec3 offset = right * side * thickness * taper;
//
//    vec4 wp = model * vec4(basePos + offset, 1.0);
//    gl_Position = projection * view * wp;
//
//    // Pass to fragment shader
//    vStrength = strength;
//    vT        = isEnd ? 1.0 : 0.0;
//    worldPos  = wp;
//}
//
//

#version 430 core

layout(std430, binding = 3) buffer RenderSSBO {
    float edgeStrength[]; // [256][49]
};

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

out float vStrength;
out float vT;
out vec4 worldPos;

void main() {
    uint id = gl_InstanceID;   // 0 .. (256*49 - 1)
    uint v  = gl_VertexID;     // 0 or 1

    uint pooledInputIdx  = id % 49;
    uint pooledHiddenIdx = id / 49;

    vec2 srcPos = vec2(pooledInputIdx % 7,  pooledInputIdx / 7)  / 7.0;
    vec2 dstPos = vec2(pooledHiddenIdx % 16, pooledHiddenIdx / 16) / 16.0;

    // distance between the two layers
    float zInput  = -0.7;
    float zHidden =  0.7;

    vec3 A = vec3(srcPos * 2.0 - 1.0, zInput);
    vec3 B = vec3(dstPos * 2.0 - 1.0, zHidden);

    vec3 pos = (v == 0) ? A : B;

    gl_Position = projection * view * model * vec4(pos, 1.0);

    // pass to fragment
    vT = float(v);
    vStrength = edgeStrength[id];
    worldPos = model * vec4(pos, 1.0);
}
