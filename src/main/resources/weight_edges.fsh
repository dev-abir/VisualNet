#version 430

in float vStrength;
in float vT;
in vec4 worldPos;

//uniform float time;

out vec4 FragColor;

void main() {
//    float alpha = smoothstep(0.0, 0.1, abs(vStrength));
//    vec3 color  = mix(vec3(0.2, 0.6, 1.0), vec3(1.0, 0.2, 0.2), step(0.0, vStrength));

    float strength = abs(vStrength);
    vec3 color = mix(vec3(1, 0, 0), vec3(0,0,1), strength);
    FragColor = vec4(color, ceil(strength));
}



//#version 430 core
//
//in float vStrength;
//in float vT;
//in vec4 vWorldPos;
//
//uniform float time;
//
//out vec4 FragColor;
//
////float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
//
//void main() {
//    // strength → brightness
//    float a = clamp(abs(vStrength * 500), 0.0, 1.0);
//    vec3 color = mix(vec3(0.2, 0.4, 1.0), vec3(1.0, 0.2, 0.2), vT);
////    vec3 color = mix(vec3(1, 1, 1.0), vec3(0.0, 1.0, 0.0), vT);
//
////    vec4 cameraPos = vec4(0, 10, 20, 1);
////    float distance = length(vWorldPos - cameraPos);
////float fogDensity=0.03;
////    vec3 fogColor = vec3(0.6f, 0.7f, 0.8f);
//    // Exponential fog
////    float fogFactor = exp(-fogDensity * distance);
////    fogFactor = clamp(fogFactor, 0.0, 1.0);
////    vec3 finalColor = mix(fogColor, color, fogFactor);
////    fragColor = vec4(finalColor, a);
//
////float noiseAmount = 0.015;
////float noiseScale = 700.0;
////
////vec2 nUV = gl_FragCoord.xy / noiseScale + time * 0.1;
////    float n = hash(nUV) * 2.0 - 1.0;
////
////    color += n * noiseAmount;
//
//    FragColor = vec4(color, a);
//}
