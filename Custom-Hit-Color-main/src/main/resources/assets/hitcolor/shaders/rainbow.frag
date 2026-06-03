#version 120

uniform float round;
uniform vec2 size;
uniform vec4 color;
uniform float time;
uniform float rainbowSpeed;

float alpha(vec2 d, vec2 d1) {
    vec2 v = abs(d) - d1 + round;
    return min(max(v.x, v.y), 0.0) + length(max(v, .0f)) - round;
}

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
    vec2 centre = .5f * size;
    vec2 coords = gl_TexCoord[0].st * size;
    
    float baseAlpha = 1.f - smoothstep(0.f, 1.5f, alpha(centre - coords, centre - 1.f));
    
    float hue = fract(time * rainbowSpeed + coords.x * 0.01 + coords.y * 0.01);
    vec3 rainbow = hsv2rgb(vec3(hue, 0.7, 1.0));
    
    vec3 finalColor = mix(color.rgb, rainbow, 0.6);
    gl_FragColor = vec4(finalColor, color.a * baseAlpha);
}
