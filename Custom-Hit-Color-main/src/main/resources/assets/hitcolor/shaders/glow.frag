#version 120

uniform float round;
uniform vec2 size;
uniform vec4 color;
uniform vec2 glowPos;
uniform float glowRadius;
uniform vec4 glowColor;
uniform float glowIntensity;

float alpha(vec2 d, vec2 d1) {
    vec2 v = abs(d) - d1 + round;
    return min(max(v.x, v.y), 0.0) + length(max(v, .0f)) - round;
}

void main() {
    vec2 centre = .5f * size;
    vec2 coords = gl_TexCoord[0].st * size;
    
    float baseAlpha = 1.f - smoothstep(0.f, 1.5f, alpha(centre - coords, centre - 1.f));
    
    float dist = distance(coords, glowPos);
    float glow = exp(-dist * dist / (glowRadius * glowRadius)) * glowIntensity;
    
    vec3 finalColor = mix(color.rgb, glowColor.rgb, glow * glowColor.a);
    gl_FragColor = vec4(finalColor, color.a * baseAlpha);
}
