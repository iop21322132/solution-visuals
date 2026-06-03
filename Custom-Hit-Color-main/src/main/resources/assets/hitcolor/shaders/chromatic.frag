#version 120

uniform float round;
uniform vec2 size;
uniform vec4 color;
uniform float time;
uniform float chromaticAberration;

float alpha(vec2 d, vec2 d1) {
    vec2 v = abs(d) - d1 + round;
    return min(max(v.x, v.y), 0.0) + length(max(v, .0f)) - round;
}

void main() {
    vec2 centre = .5f * size;
    vec2 coords = gl_TexCoord[0].st * size;
    vec2 uv = gl_TexCoord[0].st;
    
    float baseAlpha = 1.f - smoothstep(0.f, 1.5f, alpha(centre - coords, centre - 1.f));
    
    vec2 direction = uv - vec2(0.5);
    float dist = length(direction);
    
    vec2 offset = direction * dist * chromaticAberration * (1.0 + sin(time) * 0.2);
    
    float r = 1.f - smoothstep(0.f, 1.5f, alpha(centre - (uv + offset) * size, centre - 1.f));
    float g = baseAlpha;
    float b = 1.f - smoothstep(0.f, 1.5f, alpha(centre - (uv - offset) * size, centre - 1.f));
    
    vec3 finalColor = vec3(r, g, b) * color.rgb;
    gl_FragColor = vec4(finalColor, color.a * baseAlpha);
}
