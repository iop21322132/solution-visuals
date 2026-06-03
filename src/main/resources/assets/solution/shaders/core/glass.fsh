#version 150

uniform vec2 size;
uniform vec2 location;
uniform vec4 radius;
uniform float thickness;
uniform float softness;

uniform sampler2D InputSampler;
uniform vec2 InputResolution;
uniform float Quality;
uniform float Distortion;

uniform vec4 color1;
uniform vec4 color2;
uniform vec4 color3;
uniform vec4 color4;
uniform vec4 outlineColor;

in vec2 texCoord;
out vec4 fragColor;

float roundedBoxSDF(vec2 center, vec2 size, vec4 radius) {
    radius.xy = (center.x > 0.0) ? radius.xy : radius.zw;
    radius.x  = (center.y > 0.0) ? radius.x : radius.y;

    vec2 q = abs(center) - size + radius.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius.x;
}

vec4 createGradient(vec2 coords, vec4 color1, vec4 color2, vec4 color3, vec4 color4){
    vec4 color = mix(mix(color1, color2, coords.y), mix(color3, color4, coords.y), coords.x);
    color += mix(0.0019607843, -0.0019607843, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));
    return color;
}

vec4 glassBlur() {
    #define GOLDEN_ANGLE 2.39996323
    #define NUM_SAMPLES 32.0

    // ── Coordinates and basic colors ────────────────────────────────────────
    vec2 uv = gl_FragCoord.xy / InputResolution.xy;
    vec2 relPos = gl_FragCoord.xy - (location + size / 2.0);
    float sdf = roundedBoxSDF(relPos, size / 2.0, radius);

    // Bevel normal & profile (defines the edge of the glass sheet)
    float bevelWidth = 16.0;
    float edgeFactor = clamp(1.0 - abs(sdf) / bevelWidth, 0.0, 1.0);
    float bevelProfile = pow(edgeFactor, 1.5);

    vec2 eps = vec2(1.0, 0.0);
    float dx = roundedBoxSDF(relPos + eps.xy, size / 2.0, radius)
             - roundedBoxSDF(relPos - eps.xy, size / 2.0, radius);
    float dy = roundedBoxSDF(relPos + eps.yx, size / 2.0, radius)
             - roundedBoxSDF(relPos - eps.yx, size / 2.0, radius);
    vec2 normal = (length(vec2(dx, dy)) > 0.0001) ? normalize(vec2(dx, dy)) : vec2(0.0);

    // ── Distortions (Subtle Edge Refraction) ────────────────────────────────
    // Bends light inwards at the beveled edge, scaled by the Distortion uniform
    vec2 edgeOffset = -normal * (bevelProfile * Distortion * 0.8) / InputResolution.xy;

    // ── Chromatic Aberration (Edge-relative dispersion) ─────────────────────
    // Dispersion only occurs where light refracts (at the beveled edges)
    float dispersion = 0.08 * Distortion;
    
    // UVs for Red, Green, and Blue channels
    vec2 uvR = uv + edgeOffset * (1.0 - dispersion);
    vec2 uvG = uv + edgeOffset;
    vec2 uvB = uv + edgeOffset * (1.0 + dispersion);

    // ── Sampling the blurred background ─────────────────────────────────────
    vec2 Radius = Quality / InputResolution.xy;
    vec3 blurColor = vec3(0.0);
    for (float i = 0.0; i < NUM_SAMPLES; i++) {
        float r     = sqrt((i + 0.5) / NUM_SAMPLES);
        float theta = i * GOLDEN_ANGLE;
        vec2 offset = vec2(cos(theta), sin(theta)) * r * Radius;

        blurColor.r += texture(InputSampler, clamp(uvR + offset, 0.001, 0.999)).r;
        blurColor.g += texture(InputSampler, clamp(uvG + offset, 0.001, 0.999)).g;
        blurColor.b += texture(InputSampler, clamp(uvB + offset, 0.001, 0.999)).b;
    }
    blurColor /= NUM_SAMPLES;

    // ── Specular Bevel Highlight & Shadow (3D lighting) ────────────────────
    float light = dot(normal, normalize(vec2(-1.0, 1.0)));
    float highlight = smoothstep(0.0, 1.0, light) * bevelProfile * 0.15;
    float shadow = smoothstep(0.0, 1.0, -light) * bevelProfile * 0.08;

    // ── Surface linear sheen ────────────────────────────────────────────────
    vec2 coords = (gl_FragCoord.xy - location) / size;
    float sheen = smoothstep(0.3, 0.7, sin((coords.x + coords.y) * 1.5 - 0.5)) * 0.02;

    // ── Tactile grain noise ─────────────────────────────────────────────────
    float noise = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453);
    vec3 grain = vec3(noise - 0.5) * 0.012;

    // Add lighting & sheen to the blurred background
    vec3 glassColor = blurColor + vec3(highlight) - vec3(shadow) + vec3(sheen) + grain;

    // Tint color from CPU
    vec4 rectColor = createGradient(coords, color1, color2, color3, color4);

    // Blend the frosted glass background with the semi-transparent panel color
    vec3 finalColor = mix(glassColor, rectColor.rgb, rectColor.a);

    return vec4(finalColor, rectColor.a);
}

void main() {
    float distance = roundedBoxSDF(gl_FragCoord.xy - location - (size / 2.0), size / 2.0, radius);
    float smoothedAlpha = 1.0 - smoothstep(-1.0, thickness > 0. ? 1. : softness + 1., distance);

    vec2 coords = (gl_FragCoord.xy - location) / size;
    vec4 rectColor = createGradient(coords, color1, color2, color3, color4);

    if (smoothedAlpha < 0.49 && thickness > 0.) {
        float smoothedborderAlpha = (1.0 - smoothstep(-softness, softness, distance));
        fragColor = vec4(outlineColor.rgb, smoothedborderAlpha * outlineColor.a);
    } else {
        float borderAlpha = 1.0 - smoothstep(thickness - 2.0, thickness, abs(distance));
        vec4 blur = glassBlur();
        
        // Output smoothedAlpha for opacity so the GPU draws the glass panel opaque,
        // preventing the sharp background from leaking through.
        vec4 basicColor = vec4(blur.rgb, smoothedAlpha);
        fragColor = mix(vec4(blur.rgb, 0.0), mix(basicColor, thickness > 0. ? outlineColor : basicColor, borderAlpha), smoothedAlpha);
    }

    // Multiply by the client UI fade alpha so the glass/blur disappears simultaneously
    fragColor.a *= rectColor.a;
}
