#version 120

uniform float round;
uniform vec2 size;
uniform vec4 color;
uniform float time;
uniform float scanlineIntensity;
uniform float scanlineCount;

float alpha(vec2 d, vec2 d1) {
    vec2 v = abs(d) - d1 + round;
    return min(max(v.x, v.y), 0.0) + length(max(v, .0f)) - round;
}

void main() {
    vec2 centre = .5f * size;
    vec2 coords = gl_TexCoord[0].st * size;
    
    float baseAlpha = 1.f - smoothstep(0.f, 1.5f, alpha(centre - coords, centre - 1.f));
    
    float scanline = sin(coords.y * scanlineCount + time * 2.0) * 0.5 + 0.5;
    scanline = pow(scanline, 2.0) * scanlineIntensity;
    
    vec3 finalColor = color.rgb * (1.0 - scanline * 0.3);
    gl_FragColor = vec4(finalColor, color.a * baseAlpha);
}
