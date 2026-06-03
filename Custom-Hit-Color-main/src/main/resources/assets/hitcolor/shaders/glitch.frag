#version 120

uniform float round;
uniform vec2 size;
uniform vec4 color;
uniform float time;
uniform float glitchIntensity;

float alpha(vec2 d, vec2 d1) {
    vec2 v = abs(d) - d1 + round;
    return min(max(v.x, v.y), 0.0) + length(max(v, .0f)) - round;
}

float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

void main() {
    vec2 centre = .5f * size;
    vec2 coords = gl_TexCoord[0].st * size;
    
    float baseAlpha = 1.f - smoothstep(0.f, 1.5f, alpha(centre - coords, centre - 1.f));
    
    float glitch = step(0.98, random(vec2(time, floor(coords.y * 0.1))));
    float offset = glitch * glitchIntensity * (random(vec2(time, coords.y)) - 0.5) * 10.0;
    
    vec3 finalColor = color.rgb;
    finalColor.r = color.r + offset * 0.1;
    finalColor.b = color.b - offset * 0.1;
    
    gl_FragColor = vec4(finalColor, color.a * baseAlpha);
}
