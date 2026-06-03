#version 120

uniform float round;
uniform vec2 size;
uniform vec4 color;
uniform float shadowRadius;
uniform float shadowSoftness;
uniform vec2 shadowOffset;

float alpha(vec2 d, vec2 d1) {
    vec2 v = abs(d) - d1 + round;
    return min(max(v.x, v.y), 0.0) + length(max(v, .0f)) - round;
}

void main() {
    vec2 centre = .5f * size;
    vec2 coords = gl_TexCoord[0].st * size;
    
    float baseAlpha = 1.f - smoothstep(0.f, 1.5f, alpha(centre - coords, centre - 1.f));
    
    vec2 shadowCoords = coords - shadowOffset;
    float shadowDist = alpha(centre - shadowCoords, centre - 1.f + shadowRadius);
    float shadow = smoothstep(-shadowSoftness, shadowSoftness, shadowDist);
    
    vec3 finalColor = mix(vec3(0.0, 0.0, 0.0), color.rgb, baseAlpha);
    float finalAlpha = mix(color.a * 0.3, color.a, shadow) * baseAlpha;
    
    gl_FragColor = vec4(finalColor, finalAlpha);
}
